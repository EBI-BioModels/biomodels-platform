/**
 * Copyright (C) 2010-2016 EMBL-European Bioinformatics Institute (EMBL-EBI),
 * Deutsches Krebsforschungszentrum (DKFZ)
 *
 * This file is part of Jummp.
 *
 * Jummp is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along
 * with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
 **/

import grails.converters.JSON
import groovy.sql.Sql
import groovyx.gpars.GParsPool
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.locks.ReentrantLock
import java.util.regex.Pattern
import net.biomodels.jummp.core.model.ModelState
import net.biomodels.jummp.core.model.ValidationState
import org.springframework.orm.hibernate4.SessionHolder
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.transaction.support.TransactionSynchronizationManager

includeTargets << grailsScript("_GrailsArgParsing")
includeTargets << grailsScript("_GrailsBootstrap")

/**
 * Provides a mechanism for importing models into JUMMP from a user-specified location.
 *
 * The location of the folder containing all models can be specified as a command-line
 * argument to this script and could be extended in the future to accept an environment
 * variable as an alternative.
 *
 * The other argument this script expects is the path to a file with the following format
 * {username:<username>, password:<unencrypted_password>}
 * These credentials should belong to a user with an active JUMMP account on whose behalf
 * the submissions will be made.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 */

/*
 * The account of the user who is submitting the models
 */
String username
/**
 * The password in plain-text that the user supplies.
 * For added security, this can be specified in a file that is only readable by the file owner.
 */
String password
/**
 * The target directory containing all the models we wish to import.
 */
File modelFolder
/**
 * The directory where all models are stored.
 */
String workingDirectory
/**
 * The location of the exchange directory that is required by the current VcsManager implementation.
 */
String exchangeDirectory
/**
 * The authentication details of the user running the batch import
 *
 * Populated as part of the script's bootstrap, in prepareImporter().
 */
def adminAuthenticationDetails

/*
 * BioModels database credentials
 */
String bmServer
String bmPort
String bmDB
String bmUsername
String bmPassword

/*
 * WebAuth database credentials
 */
String authServer
String authPort
String authDB
String authUsername
String authPassword

/**
 * Groovy SQL connections to BioModels model- and auth databases respectively
 */
Sql biomodelsConnection
Sql authConnection

/**
 * Location of simulation files
 */
File simulationFolder

/**
 * Cache of users used in this import which maps their email to their id.
 *
 * Since users in this map may be reused in different threads, and hence different
 * Hibernate sessions, we cannot put User objects as values. This is because any subsequent
 * changes to the user object outside of its original session will NOT be persisted in the
 * database.
 */
def userCache = new ConcurrentHashMap<String, Long>()
/**
 * Guard against cache stampedes
 */
ReentrantLock userCacheModifier = new ReentrantLock()

class ModelLogger {
    Set err
    Set out

    void logMsg(def msg) {
        out << msg
    }

    void errMsg(def msg) {
        err << msg
    }

    String toString() {
        "out: $out, err: $err"
    }
}

/**
 * Log for model-related messages.
 *
 * Keys represent model identifiers. Values represent pairs of ordered sets of messages
 * corresponding to the error log and the info log respectively.
 *
 * Since all messages relating to a model will be inserted by the same thread, there is no
 * need to use locks.
 */
def messageLog = new ConcurrentHashMap<String, ModelLogger>()

LinkedBlockingQueue insertedRevisions = new LinkedBlockingQueue()


/**
 * The branches of BioModels where we look for model information.
 */
def bioModelsBranches = ["publ", "uncura_publ"]//, "anno", "uncura_anno", "cura", "auto_gen_models"]

/*
 * Domain classes that will be needed in multiple closures, declared globally,
 * instantiated in loadClasses()
 */
def User
def Role
def UserRole
def Person
def AclSid
def rftc
def mftc
def rtc
def mtc
def plptc
def Publication
def ptc
def mf
def decorator
def domainAdapter
def Model
def Revision

def ModelOfTheMonth
def CurationNotes

def ResourceReference
def Qualifier
def Statement
def ElementAnnotation
def ModelElementType
def PublicationLinkProvider
def LinkType

/**
 * The Hibernate SessionFactory
 */
def sessionFactory

/**
 * Services used by the script
 */
def pubMedService
def publicationService
def modelService
def modelFileFormatService
def userService
def springSecurityService
def aclUtilService

/**
 * Expected contents of a typical folder for literature-based models
 */
def expectedFiles = [
        "[A-Z0-9]*_urn\\.xml": "Auto-generated SBML file with URNs",
        "[A-Z0-9]*-biopax2\\.owl": "Auto-generated BioPAX (Level 2)",
        "[A-Z0-9]*-biopax3\\.owl": "Auto-generated BioPAX (Level 3)",
        "[A-Z0-9]*cellml": "Auto-generated CellML",
        "[A-Z0-9]*\\.m" : "Auto-generated Octave file",
        "[A-Z0-9]*\\.pdf" : "Auto-generated PDF file",
        "[A-Z0-9]*\\_manual.png" : "Manually generated Reaction graph (PNG)",
        "[A-Z0-9]*\\_manual.svg" : "Manually generated Reaction graph (SVG)",
        "[A-Z0-9]*\\.png" : "Auto-generated Reaction graph (PNG)",
        "[A-Z0-9]*\\.svg" : "Auto-generated Reaction graph (SVG)",
        "[A-Z0-9]*\\.sci" : "Auto-generated Scilab file",
        "[A-Z0-9]*\\.vcml" : "Auto-generated VCML file",
        "[A-Z0-9]*\\.xpp" : "Auto-generated XPP file" ]

/**
 * Known file suffixes
 */
final String DOT_XML = ".xml"
final String URL_FILE = "_url.xml"
final String ORIGIN = "${DOT_XML}.origin"

// templates for submission comments
final String ORIG_COMMENT_TPL = "Original import of "
final String UPDATE_COMMENT_TPL = "Current version of "

// BioModels branches
final String AUTO_GEN = "auto_gen_models"
final String PUBL = 'publ'
final String UNCURA_PUBL = 'uncura_publ'

/**
 * Returns a User corresponding to the submitter of the model in BioModels.
 */
getUserFromBiomodelsId = { bmPersonId ->
    def personDetails = authConnection.firstRow(
            "select * from auth_persons where person_id = ?", [bmPersonId])
    if (!personDetails || !personDetails.email) {
        return null
    }
    def existing = User.findByEmail(personDetails.email)
    if (existing) {
        return existing
    }
    String personName = "${personDetails.given_name} ${personDetails.family_name}"
    def person = Person.newInstance(userRealName: personName,
            institution: personDetails.organisation)
    def userCreated = User.newInstance(person: person,
            username: getUsername(personDetails), password: "autocreated",
            email: personDetails.email)
    if (!userCreated.validate()) {
        error("Cannot create account for submitter $bmPersonId: ${userCreated.errors.allErrors}")
    }
    long userId = registerUser(userCreated)
    if (userId) {
        return User.get(userId)
    } else {
        error("failed to create user $userCreated for $personName -- BioModels Person $bmPersonId")
    }
}

/**
 * Very simple means of executing an action as a different user than the one
 * that is currently authenticated.
 *
 * Performs the action defined by closure as the user defined by Authentication
 * object auth, then reverts to the original authentication.
 *
 * Suitable for lightweight work for which we need not create a separate thread.
 */
simpleRunAs = { auth, closure ->
    def currentAuth
    try {
        currentAuth = SecurityContextHolder.context.authentication
        SecurityContextHolder.context.authentication = auth
        def result = closure.call()
        return result
    } finally {
        if (currentAuth) {
            SecurityContextHolder.context.authentication = currentAuth
        } else {
            SecurityContextHolder.clearContext()
        }
    }
}

registerUser = { user ->
    simpleRunAs( adminAuthenticationDetails, {
        userService.register(user, true)
    })
}

getDetailsForLoggedInUser = { ->
    SecurityContextHolder.context.authentication
}

/**
 * Extracts the curation notes corresponding to the model from BioModels that we are importing.
 */
setCurationNotes = { modelSubmitted ->
    String modelId = modelSubmitted.submissionId
    def row = biomodelsConnection.firstRow("""\
SELECT * FROM simulations WHERE model_id = :mid """, [mid: modelId])
    if (row) {
        def submitterId = row.submitter_id
        def modifierId = row.modifier_id
        def submitter = getUserFromBiomodelsId(submitter_id)
        if (!submitter) {
            addModelError(modelId,
                    "Could not find submitter with id: $submitterId, curation notes not imported")
            return
        }
        def modifier
        if (submitterId == modifierId) {
            modifier = submitter
        } else {
            modifier = getUserFromBiomodelsId(modifierId)
            if (!modifier) {
                 addModelError(modelId,
                        "Could not find modifier with id: $modifierId, curation notes not imported")
                 return
             }
        }

        def notes = CurationNotes.newInstance(
                model: modelSubmitted,
                submitter: submitter,
                lastModifier: modifier,
                dateAdded: row.submission_date,
                lastModified: row.last_modification_date,
                comment: row.comments,
                curationImage: new File(simulationFolder, row.file_name).getBytes())
        notes.save()
     }
}

/**
 * Uses the Grails classloader to load a class based on its fully-qualified domain name (fqdn).
 */
loadClass = { String fqdn ->
    grailsApp.classLoader.loadClass(fqdn)
}

target(bootstrapJummp: 'Creates a fully-initialised JUMMP environment loaded with seed data') {
    bootstrap() // grails bootstrapping

    // load necessary classes
    loadClasses()

    // don't send registration confirmation emails to model submitters
    grailsApp.config.jummp.security.registration.email.send = false

    // initialise settings, authenticate using supplied credentials
    prepareImporter()

    // initialise Jummp and BioModels database connections
    prepareDataSources()

    // bootstrap code and plugins' doWithSpring closure not executed, call relevant parts manually
    populatePublicationLinkProviders()
    registerFormatHandlers()
    fixValidationForExternalDomainClasses()
}

target(registerFormatHandlers: 'Ensure we register SBML and Unknown as valid formats') {
    def defaultFormat = modelFileFormatService.registerModelFormat("UNKNOWN", "UNKNOWN")
    modelFileFormatService.handleModelFormat(defaultFormat, "unknownFormatService", "unknown")
    ["*", "L1V1", "L1V2", "L2V1", "L2V2", "L2V3", "L2V4", "L3V1"].each {
        def modelFormat = modelFileFormatService.registerModelFormat("SBML", "SBML", it)
            modelFileFormatService.handleModelFormat(modelFormat, "sbmlService", "sbml")
    }
}

target(fixValidationForExternalDomainClasses:
        'Wires the validator beans that GORM expects for our [externalised] domain classes') {
    /*
    * Issue: Domain class constraints werent being applied, leading to the
    * familiar issue of mime types not being set. Fixed by applying them
    * as below.
    */
    def domainClassGrailsPlugin = grailsApp.classLoader.loadClass(
            "org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin")
    grailsApp.domainClasses.each { gc ->
        domainClassGrailsPlugin.addValidationMethods(grailsApp,
                gc, grailsApp.mainContext)
    }
}

createRoleIfNecessary = { String authority ->
    if (!Role.findByAuthority(authority)) {
        Role.newInstance(authority: authority).save()
    }
}

target(createDefaultUserAndRoles: 'Creates the admin account and all roles') {
    User.withTransaction {
        ['ROLE_USER', 'ROLE_CURATOR', 'ROLE_ADMIN', 'ROLE_QC_PROVIDER'].each { r ->
            createRoleIfNecessary r
        }

        if (!User.findByUsername('administrator')) {
            def person = Person.newInstance(userRealName: "administrator")
            person.save()
            def user = User.newInstance(username: "administrator",
                    password: springSecurityService.encodePassword("administrator"),
                    email: "user@test.com",
                    person: person,
                    enabled: true,
                    accountExpired: false,
                    accountLocked: false,
                    passwordExpired: false)
            user.save()
            AclSid.newInstance(sid: user.username, principal: true).save()
            ['ROLE_USER', 'ROLE_ADMIN'].each { r ->
                def role = Role.findByAuthority(r)
                UserRole.create(user, role, false)
            }
        }
    }
    User.withNewSession {
        assert User.findByUsername('administrator')
    }
}

target(prepareImporter: 'Preparations for running the script -- CLI args, environment setup') {
    int inputIssues = sanitiseInput()
    if (inputIssues) {
        error("""There was a problem parsing the input parameters so I'm giving up. \
Sorry about that.""", inputIssues)
    }
    int configIssues = resetConfiguration()
    if (configIssues) {
        error("""Your \$HOME/.jummp.properties does not look right. Perhaps the \
working or exchange folders do not exist?""", configIssues)
    }
    int vcsIssues = vcsSetup()
    if (vcsIssues) {
        error("""There was a problem configuring Jummp's model versioning so I'm \
giving up. Sorry about that.""", vcsIssues)
    }

    // must ensure roles are set up before attempting to authenticate
    createDefaultUserAndRoles()

    int authIssues = authenticate(username, password)
    adminAuthenticationDetails = getDetailsForLoggedInUser()
    if (authIssues) {
        error "Wrong auth credentials. Why don't you try again?", authIssues
    }
}

// to avoid lazy initialization exceptions, call this on all worker threads
//Binds a Hibernate Session to the current thread
openSession = {
    def session = sessionFactory.openSession()
    TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session))
}

// Clears transaction synchronisations and closes active Hibernate session
closeSession = {
    def session = sessionFactory.currentSession
    if (!session) {
        String name = Thread.currentThread().name
        error("$name: No active session found for current thread -- skipping Hibernate cleanup.")
        return
    }
    session.flush()
    session.clear()
    session.close()
    TransactionSynchronizationManager.unbindResourceIfPossible(sessionFactory)
}

target(prepareDataSources: "initialisation of machinery for database interaction") {
    openSession()
    // Instantiate direct connections to DB
    def bmUrl = "jdbc:mysql://${bmServer}:${bmPort}/${bmDB}"
    def aUrl = "jdbc:mysql://${authServer}:${authPort}/${authDB}"
    biomodelsConnection = Sql.newInstance(bmUrl,
            bmUsername, bmPassword, "com.mysql.jdbc.Driver")
    authConnection = Sql.newInstance(aUrl,
            authUsername, authPassword, "com.mysql.jdbc.Driver")
}

target(loadClasses: 'Loads required classes in the Jummp Grails environment') {
    // transport commands
    rftc = loadClass("net.biomodels.jummp.core.model.RepositoryFileTransportCommand")
    mftc = loadClass("net.biomodels.jummp.core.model.ModelFormatTransportCommand")
    rtc = loadClass("net.biomodels.jummp.core.model.RevisionTransportCommand")
    mtc = loadClass("net.biomodels.jummp.core.model.ModelTransportCommand")
    plptc = loadClass("net.biomodels.jummp.core.model.PublicationLinkProviderTransportCommand")
    ptc = loadClass "net.biomodels.jummp.core.model.PublicationTransportCommand"
    Publication = loadClass "net.biomodels.jummp.model.Publication"

    // submission-related domain classes
    Person = loadClass("net.biomodels.jummp.plugins.security.Person")
    User = loadClass("net.biomodels.jummp.plugins.security.User")
    Role = loadClass("net.biomodels.jummp.plugins.security.Role")
    UserRole = loadClass("net.biomodels.jummp.plugins.security.UserRole")
    AclSid = loadClass("grails.plugin.springsecurity.acl.AclSid")
    mf = loadClass("net.biomodels.jummp.model.ModelFormat")
    mf = loadClass("net.biomodels.jummp.model.ModelFormat")
    mf = loadClass("net.biomodels.jummp.model.ModelFormat")
    Revision = loadClass("net.biomodels.jummp.model.Revision")
    PublicationLinkProvider = loadClass("net.biomodels.jummp.model.PublicationLinkProvider")
    LinkType = PublicationLinkProvider.classes[0] // the only internal class...
    Model = loadClass("net.biomodels.jummp.model.Model")
    decorator = loadClass("net.biomodels.jummp.core.model.identifier.decorator.AbstractAppendingDecorator")

    // annotation-related classes
    ResourceReference = loadClass("net.biomodels.jummp.annotationstore.ResourceReference")
    Statement = loadClass("net.biomodels.jummp.annotationstore.Statement")
    ElementAnnotation = loadClass("net.biomodels.jummp.annotationstore.ElementAnnotation")
    Qualifier = loadClass("net.biomodels.jummp.annotationstore.Qualifier")
    ModelElementType = loadClass("net.biomodels.jummp.model.ModelElementType")
    // BioModels-specific domain classes
    CurationNotes = loadClass("net.biomodels.jummp.deployment.biomodels.CurationNotes")
    ModelOfTheMonth = loadClass("net.biomodels.jummp.deployment.biomodels.ModelOfTheMonth")

    // DomainClass -> TransportCommand converter
    domainAdapter = loadClass("net.biomodels.jummp.core.adapters.DomainAdapter")

    // inject applicationContext in POGOs that expect it
    decorator.context = appCtx
    rtc.context = appCtx
    sessionFactory = appCtx.sessionFactory
    modelService = appCtx.modelService
    publicationService = appCtx.publicationService
    pubMedService = appCtx.pubMedService
    modelFileFormatService = appCtx.modelFileFormatService
    userService = appCtx.userService
    springSecurityService = appCtx.springSecurityService
    aclUtilService = appCtx.aclUtilService
}

// keep track of the number of models that are processed
def processedCount = new AtomicLong()
def failureCount = new AtomicLong()
target(main: "Puts everything together to import models from a given folder") {
    bootstrapJummp()

    def modelFolderPattern = ~/(MODEL|BIOMD)\d{10}|BMID\d{12}/

    log("${new Date()} -- commencing batch import")
    long duration = System.currentTimeMillis()
    // the size of the thread pool -- assumes a hyper-threading CPU
    // at most 48 workers since we have a limit of 50 JDBC connections
    final int POOL_SIZE = Math.min(48, 2 * Runtime.getRuntime().availableProcessors())
    log("Pool size is $POOL_SIZE")
    GParsPool.withPool(POOL_SIZE) {
        GParsPool.runForkJoin(modelFolder) { File root ->
            final String rootName = root.name
            if (rootName ==~ modelFolderPattern) {
                processModelFolder root
            } else { // fork dedicated task for each subfolder
                def subFolders = root.listFiles(new FileFilter() {
                    boolean accept(File candidate) {
                        candidate.isDirectory()
                    }
                })
                subFolders.each { File child ->
                    forkOffChild child
                }
            }
        }
    }

    duration = (System.currentTimeMillis() - duration) / 1000 /* duration in ms */
    String formattedDuration = prettify(duration)
    printModelLog()
    log("Imported ${processedCount.get()} models (${failureCount.get()} failures) in $formattedDuration")
    cleanup()
    return 0
}

printModelLog = {
    messageLog.keySet().sort().each { modelId ->
        def logger = messageLog[modelId]
        def infoMessages = logger.out
        infoMessages.each { m -> log("$modelId: $m") }
    }
    if (failureCount.get()) {
        log("Failed to import the following models:")
        messageLog.keySet().sort().each { modelId ->
            def logger = messageLog[modelId]
            def errorMessages = logger.err
            errorMessages.each { m -> error("$modelId: $m") }
        }
    }
}

processModelFolder = { File folder ->
    final String MODEL_ID = folder.name
    // find branch
    final String BRANCH = getBranch MODEL_ID
    if (!BRANCH) {
        addModelError(MODEL_ID, "Can not find $MODEL_ID in any of $bioModelsBranches")
        failureCount.incrementAndGet()
        return
    }
    boolean shouldDefer = isModelInUncuraPublAndPubl(MODEL_ID, BRANCH)
    if (shouldDefer) {
        addModelError MODEL_ID, "Entry found both in $BRANCH branch and also in publ."
        return
    }
    processedCount.incrementAndGet()
    // check symlink
    boolean haveSymlink = haveSymlinkToUrlFile folder, MODEL_ID
    if (!haveSymlink) {
        addModelError MODEL_ID, "${folder} does not contain a symbolic link to the URL file"
    }
    // separate original file from the rest of the folder contents
    def originalFile = findOriginalFile(folder, MODEL_ID)
    if (!originalFile) {
        addModelError(MODEL_ID, "Original submission file not found")
        failureCount.incrementAndGet()
        return
    }
    // find submissionInfo
    def modelDetails = getModelDetails MODEL_ID, BRANCH
    if (!modelDetails) {
        addModelError(MODEL_ID, "Error retrieving model details from BioModels DB")
        failureCount.incrementAndGet()
        return
    }
    try {
        openSession()
        def submitter
        authenticate(username, password)
        // create a Jummp account for submitter
        try {
            submitter = getUser MODEL_ID, BRANCH
        } catch (Exception e) {
            addModelError(MODEL_ID, "Can't get submitter account for MODEL $MODEL_ID ($BRANCH) :: $e")
        } finally {
            logOut()
        }

        if (!submitter) {
            addModelError(MODEL_ID, "No user found, please check details in BioModels DB")
            failureCount.incrementAndGet()
            return
        }
        authenticateAsUser(submitter)
        // submit first revision as *.origin
        def submittedModel = submitOriginalFile(BRANCH, MODEL_ID, originalFile, modelDetails)
        if (!submittedModel) {
            addModelError(MODEL_ID, "Error importing original file")
            failureCount.incrementAndGet()
            return
        }
        // submit second revision as * without original file
        def revision = addRevision(MODEL_ID, folder, submittedModel)
        if (!revision || revision?.hasErrors()) {
            def err = revision?.errors?.allErrors
            addModelError(MODEL_ID, "Could not update original submission: $err")
            failureCount.incrementAndGet()
            return
        }
        addRevisionAnnotations(revision, BRANCH, modelDetails, submitter)
        def revisions = [submittedModel.revisions[0], revision]
        revisions.each { r ->
            publishModelRevision(MODEL_ID, r)
        }
        submittedModel.revisions.each { r ->
            insertedRevisions.offer(r.id)
        }
    } catch (Throwable t) {
        addModelError(MODEL_ID, "Something went wrong with ${MODEL_ID} - ${t}")
        failureCount.incrementAndGet()
    } finally {
        closeSession()
        logOut()
    }
}

/**
 * Uploads the original file of the BioModels submission that is being processed.
 *
 * @param branch the branch of BioModels containing this model: curated, noncurated, auto-generated
 * @param modelId the submission identifier that should be used for this model
 * @param originalFile a file that should be uploaded in Jummp
 * @param infoMap model details as extracted from BioModels -- see getModelDetails()
 *
 * @throws IllegalStateException if there is no auth token for the current thread.
 *
 * @return the Model instance that was just created and persisted.
 */
submitOriginalFile = { branch, modelId, originalFile, infoMap ->
    if (!SecurityContextHolder.context) {
        def msg = "Cannot submit original version of $modelId -- missing security context"
        throw new IllegalStateException(msg.toString())
    }
    def originInfo = getSubmissionData(originalFile, [], ORIG_COMMENT_TPL)
    def files = getFilesFromSubmissionData originInfo
    def revisionCmd = originInfo.get("revision")
    def model = modelService.uploadValidatedModel(files, revisionCmd)
    if (model?.hasErrors()) {
        def e = m?.errors?.allErrors
        addModelError modelId, "Submission of original file with $infoMap failed -- ${e}"
        return null
    }
    // modify database to match the information from BioModels about this deposition
    def uploadDate = infoMap['submissionDate']
    def firstPublished = infoMap['publicationDate']
    def publicationId = infoMap['biomodels_id']
    def submissionId = infoMap['model_id']
    def inPublBranch = isCuratedAndPublished(branch)
    if ( inPublBranch && !publicationId) {
        throw new IllegalStateException("No BIOMD* found for curated model $modelId".toString())
    }
    if (publicationId) {
        model.publicationId = publicationId
        model.submissionId = submissionId
        model.firstPublished = firstPublished
    } else {
        model.submissionId = submissionId
    }
    model.revisions[0].uploadDate = uploadDate

    if (AUTO_GEN != branch) {
        processModelOfTheMonth(model)
        setCurationNotes(model)
    }

    if (!model.save(flush: true) && model.hasErrors()) {
        def err = model.errors.allErrors
        addModelError(modelId, "Validation errors when persisting original submission: $err")
        return
    }
    addModelMsg(modelId, "Original submission successfully imported")
    return model
}

isCuratedAndPublished = { branch ->
    PUBL  == branch
}

isNotCuratedAndPublished = { branch ->
    UNCURA_PUBL == branch
}

addRevision = { modelId, parent, model ->
    if (!model.validate()) {
        def err = model.errors.allErrors
        addModelError(modelId, "Refusing to update invalid model $modelId: $err")
        return null
    }
    def revisionInfo = prepareRevision(modelId, parent, model)
    def revision
    try {
        revision = modelService.addValidatedRevision(revisionInfo.files, [], revisionInfo.revision)
        addModelMsg modelId, "Added revision $revision"
    } catch(Exception e) {
        addModelError(modelId, "Exception thrown while updating original submission: $e")
    }
    revision
}

addRevisionAnnotations = { revision, branch, modelDetails, user ->
    boolean inPubl = isCuratedAndPublished(branch)
    String author = user.person.userRealName
    createBMAnnotation(revision, inPubl, 'curated', author)
    String jws = modelDetails['jwsLink']
    if (jws) {
        createBMAnnotation(revision, jws, 'onlineSimulation', author)
    }
    def publicationId = getPublicationIdFromModelDetails(modelDetails)
    def publicationType = getPublicationTypeFromModelDetails(modelDetails)
    boolean havePublication = null != publicationId && null != publicationType
    if (havePublication) {
        addPublicationDetails(revision.model, publicationId, publicationType)
        String publicationURI = getPublicationLink(publicationId, publicationType)
        createBMAnnotation(revision, publicationURI, "originalModel", author)
    }
    def lastModified = modelDetails['lastModified']
    revision.uploadDate = lastModified
    revision.save()
}

getPublicationIdFromModelDetails = { details -> details?.publication_id }
getPublicationTypeFromModelDetails = { details -> details?.publication_id_type }

addPublicationDetails = { model, accession, type ->
    def id = model.publicationId ?: model.submissionId
    try {
        def publicationCmd = pubMedService.fetchPublicationData accession
        if (!publicationCmdHasRequiredFields(publicationCmd)) {
            addModelMsg id, "Attempting to manually populate details for $accession"
            fetchMissingPaperDetailsFromBioModels(id, publicationCmd, accession, type)
            addModelMsg id, "The publication is now ${publicationCmd.properties}"
        }
        def publication = publicationService.fromCommandObject publicationCmd
        if (!publication.validate()) {
            def e = publication.errors.allErrors
            addModelError id, "Couldn't attach publication $accession: $e"
        } else {
            model.publication = publication
            model.save()
            addModelMsg id, "Successfully added publication $accession"
        }
    } catch (Exception e) {
        addModelError id, "Could not extract details for publication with identifier $accession. $e"
    }
}

publicationCmdHasRequiredFields = { publicationCmd ->
    null == ['title', 'journal', 'affiliation', 'synopsis'].find { f ->
        !publicationCmd?."$f"
    }
}

/**
 * Attempts to add any publication details not automatically retrieved
 * from external sources by looking in BioModels.
 */
fetchMissingPaperDetailsFromBioModels = { modelId, partialPublication, accession, type ->
    def paperDetails = biomodelsConnection.firstRow """\
select title, journal_name as journal, affiliation, abstract as synopsis, year
from publications
where id_type = ? and publication_id = ?""", [type, accession]

    partialPublication.year = paperDetails.year
    ['title', 'journal', 'affiliation', 'synopsis'].each { String f ->
        String value = paperDetails."$f"
        try {
            setPublicationAttribute(modelId, partialPublication, f, value)
        } catch (Exception e) {
            addModelError modelId, "Failed to set '$f' to '$value' for publication $accession"
        }
    }
    if (isNotPubMedPublication(type)) {
        // deal with the fact that pubMedService returns a publication command with link type PUBMED
        def provider
        switch(type) {
            case 1:
                provider = PublicationLinkProvider.findByLinkType(LinkType.DOI)
            break
            case 2:
                provider = PublicationLinkProvider.findByLinkType(LinkType.CUSTOM)
            break
            default:
                String m = "Publication $accession ($modelId) has unsupported type $type"
                throw new IllegalStateException(m)
        }
        def providerCmd = plptc.newInstance(linkType: provider.linkType, pattern: provider.pattern,
                identifiersPrefix: provider.identifiersPrefix)
        partialPublication.linkProvider = providerCmd
    }
}

boolean isNotPubMedPublication(int type) {
    type > 0
}

setPublicationAttribute = { modelId, publicationCmd, field, value ->
    if (!publicationCmd."$field") {
        publicationCmd."$field" = value ?: ""
        addModelMsg modelId, "set publication field $field to ${publicationCmd."$field"}"
    }
}

getOriginalFileForModel = { folder, id ->
    new File(folder, "$id$ORIGIN")
}

getSymlinkFileForModel = { folder, id ->
    new File(folder, "$id$DOT_XML")
}

getUrlFileForModel = { folder, id ->
    new File(folder, "$id$URL_FILE")
}

/**
 * Returns whether a model in uncura_publ is a duplicate of a record in publ.
 *
 * Some models appear in uncura_publ even though they have been moved to publ.
 * The models do not appear in the non-curated list in BioModels, but we need
 * to deal with the inconsistency in Jummp.
 *
 * This closure is used to ignore entries in the uncura_publ table that should
 * not be there because they are also present in publ.
 */
isModelInUncuraPublAndPubl = { id, branch ->
    if (isNotCuratedAndPublished(branch) && id.startsWith("MODEL")) {
        def result = modelExistsInPubl(id)
        return result
    }
    return false // let publ and p2m models go through
}

/**
 * Checks if there is a BIOMD id for the supplied submission identifier.
 *
 * Expects modelId to be of the form MODEL\d{10}.
 * Uses the cura table to provide this information and returns true iff there is
 * a row in cura containing modelId as model_id and something starting with "BIOMD"
 * in the biomodels_id column.
 */
modelExistsInPubl = { modelId ->
    assert modelId?.startsWith("MODEL")
    def curaRow = getModelById(modelId, "cura")
    if (!curaRow) {
        addModelError(modelId, "Expected to find the model in the cura table, but it's not there.")
        throw new IllegalStateException("Model $modelId not found in cura table".toString())
    }
    return curaRow.biomodels_id?.startsWith("BIOMD")
}

/*
 * Returns true if modelFolder contains a symlink named ${id}.xml pointing to ${id}_url.xml
 */
haveSymlinkToUrlFile = { folder, id ->
    assert folder.exists()
    File symlink = getSymlinkFileForModel(folder, id)
    File target = getUrlFileForModel(folder, id)
    symlink.exists() && target.exists() && symlink.canonicalPath == target.canonicalPath
}

findOriginalFile = { folder, id ->
    assert folder.exists()
    File origin = getOriginalFileForModel(folder, id)
    origin.exists() ? origin : null
}

// called after we ensured the original file is present in the folder
findNewestRevisionFiles = { parent, id ->
    assert parent.exists()
    def result = [:]
    def mainFile = getUrlFileForModel(parent, id)
    def originalFile = getOriginalFileForModel(parent, id)
    def symlinkFile = getSymlinkFileForModel(parent, id)
    assert mainFile.exists()
    result['mainFile'] = mainFile
    def additionalFiles = parent.listFiles().findAll { f ->
        !(f in [mainFile, originalFile, symlinkFile] )
    }
    result['additionals'] = additionalFiles
    result
}

prepareRevision = { modelId, parent, model ->
    assert !(model.hasErrors())
    def fileMap = findNewestRevisionFiles(parent, modelId)
    def main = fileMap['mainFile']
    def additionals = fileMap['additionals']
    def revisionData = getSubmissionData(main, additionals, UPDATE_COMMENT_TPL)
    def fileTCs = getFilesFromSubmissionData(revisionData)
    def revisionTC = revisionData.get("revision")
    def auth = getDetailsForLoggedInUser()
    String principal = auth.principal
    Date uploadDate = model.revisions[0].uploadDate
    def sId = model.submissionId
    def pId = model.publicationId
    revisionTC.model = createModelCommandForRevision(revisionTC, principal, uploadDate, sId, pId)
    [revision: revisionTC, files: fileTCs]
}

createModelCommandForRevision = { revision, submitterName, date, submissionId, publicationId ->
    def fmt = revision.format
    String fmtName = revision.format.identifier
    String fmtVersion = revision.format.formatVersion
    def formatCmd = mftc.newInstance(identifier: fmtName, formatVersion: fmtVersion)
    def modelCmd = mtc.newInstance( format: formatCmd, submitter: submitterName,
            submissionDate: date, submissionId: submissionId, publicationId: publicationId)
}

target(cleanup: "Shutdown hook used to gracefully close resources") {
    closeDataSources()
    closeCamel()
    expireUserPasswords()
}

target(expireUserPasswords: 'Forces users with accounts created herein to reset their passwords') {
    userCache.values().each { id ->
        userService.expirePassword(id, true)
    }
}

target(closeDataSources: "Close any active database connections") {
    closeSession()
    biomodelsConnection?.close()
    authConnection?.close()
}

target(closeCamel: "Shuts down the Camel instance, awaiting for current messages to be delivered") {
    def camelContext = appCtx.camelContext
    duration = System.currentTimeMillis()
    camelContext.shutdown()
    duration = (System.currentTimeMillis() - duration) / 1000
    log("Waited ${prettify(duration)} for Camel to stop gracefully.")
}

target(sanitiseInput: "Processes user input") {
    // provides argsMap
    parseArguments()
    def modelFolderParameter = argsMap.get("models")
    def credentialsParameter = argsMap.get("credentials")
    File credentials
    if (argsMap.size() < 3 || !modelFolderParameter || !credentialsParameter ||
            argsMap.get("params")) {
        error('''USAGE\t\t\
batch-import --models=<model_folder_location> --credentials=<path_to_credentials_file>''', 1)
    }
    File location = new File(modelFolderParameter)
    if (!location.exists() || !location.isDirectory()) {
        error "There is no directory that I can access ${location.absolutePath}", 2
    }
    modelFolder = location.getCanonicalFile()
    location = new File(credentialsParameter)
    if (!location.exists() || !location.isFile()) {
        error "Did not find any credentials in ${location.absolutePath}", 4
    }
    credentials = location.getCanonicalFile()
    def c = JSON.parse(new FileInputStream(credentials.absolutePath), "UTF8")

    simulationFolder = new File(c.'simulationFolder')

    (username, password) = [c.'username', c.'password']
    (bmServer, bmPort, bmDB, bmUsername, bmPassword) = [c.'biomodelsServer', c.'biomodelsPort',
            c.'biomodelsDB', c.'biomodelsUsername', c.'biomodelsPassword']

    (authServer, authPort, authDB, authUsername, authPassword) = [c.'authServer', c.'authPort',
            c.'authDB', c.'authUsername', c.'authPassword']
    return 0
}

target(vcsSetup: "Ensures Git is in charge of versioning models") {
    grailsApp.config.jummp.plugins.git.enabled = true
    grailsApp.config.jummp.plugins.svn.enabled = false
    grailsApp.config.jummp.vcs.pluginServiceName="gitManagerFactory"
    appCtx.getBean("vcsService").vcsManager = appCtx.getBean("gitManagerFactory").getInstance()
    assert appCtx.getBean("vcsService").isValid() == true
    return 0
}

def parseJummpConfig = {
    def props = new Properties()
    def service = appCtx.getBean("configurationService")
    String pathToConfig = service.getConfigFilePath()
    if (!pathToConfig) {
        throw new Exception("No config file available.")
    }
    props.load(new FileInputStream(pathToConfig))
    new ConfigSlurper().parse(props)
}

target(resetConfiguration: "Resets the key properties to the user-supplied defaults") {
    def jummpConfig = parseJummpConfig()
    workingDirectory = jummpConfig.jummp.vcs.workingDirectory
    def wd = new File(workingDirectory)
    if (!wd.exists() || !wd.isDirectory()) {
        workingDirectory = null
        exchangeDirectory = null
        error("""Please set jummp.vcs.workingDirectory in .jummp.properties to point \
to an empty folder""", 8)
    } else {
        grailsApp.config.jummp.vcs.workingDirectory = workingDirectory
    }
    exchangeDirectory= jummpConfig.jummp.vcs.exchangeDirectory
    def ed = new File(exchangeDirectory)
    if (!ed.exists() || !ed.isDirectory()) {
        workingDirectory = null
        exchangeDirectory = null
        error("""Please set jummp.vcs.exchangeDirectory in .jummp.properties to point to an \
empty folder""", 8)
    } else {
        grailsApp.config.jummp.vcs.exchangeDirectory = exchangeDirectory
    }
    return 0
}

target(inspectSession: 'Prints information about entities stored in a Hibernate session') {
    def session = sessionFactory.currentSession
    def stats = session.statistics
    int count = stats.entityCount
    Set keys = stats.entityKeys
    def result = new StringBuilder(8096)
    String s = System.lineSeparator()
    result.append("=== Session $session contains $count entities ===").append(s)
    keys.each { k ->
        result.append(k.identifier).append('\t\t').append(k.entityName).append(s)
    }
    log(result.toString())
}

authenticate = { user, passwd ->
    def authToken = new UsernamePasswordAuthenticationToken(user, passwd)
    def auth = appCtx.getBean("authenticationManager").authenticate(authToken)
    if (!auth.authenticated) {
        error("Are you sure that is the right username/password combination for your account?",
                16)
    }
    SecurityContextHolder.getContext().setAuthentication(auth)
    return 0
}

/**
 * Clears the authentication token for the current thread.
 *
 * It is very important that this method is called before the thread is returned to the pool
 * to avoid unexpected side-effects -- e.g. stale auth tokens being used for submitting a different
 * model than the designated one.
 */
logOut = {
    SecurityContextHolder.clearContext()
}

addModelError = { model, msg ->
    messageLog.putIfAbsent(model, new ModelLogger(err: new LinkedHashSet(), out: new LinkedHashSet()))
    def logger = messageLog[model]
    logger.errMsg msg
}

addModelMsg = { model, msg ->
    messageLog.putIfAbsent(model, new ModelLogger(err: new LinkedHashSet(), out: new LinkedHashSet()))
    def logger = messageLog[model]
    logger.logMsg msg
}

error = { msg, int code = -1 ->
    event('StatusError', [msg])
    if (code != -1) {
        exit code
    }
}

log = { msg ->
    event('StatusUpdate', [msg])
}

/*
 * Generates data structures needed for submission.
 *
 * Returns a map containing the RepositoryFileTransportCommand list and the
 * RevisionTransportCommand corresponding to this submission. The map's keys
 * are 'files' and 'revision'.
 */
getSubmissionData = { file, additional, comment ->
    def modelWrapper = rftc.newInstance(path: file.absolutePath, description: "",
            mainFile: true, userSubmitted: true, hidden: false)
    // infer model format
    def formatCommand = modelFileFormatService.inferModelFormat([modelWrapper])
    def format = mf.findByIdentifierAndFormatVersion(formatCommand.identifier,
            formatCommand.formatVersion)
    // get name and description
    final String MODEL_NAME = modelFileFormatService.extractName([file], format)?:
            new File(file.absolutePath).getName()
    modelWrapper.description = "${MODEL_NAME}"
    final String DESCRIPTION = modelFileFormatService.extractDescription([file], format)
    // validate model
    boolean isValid = modelFileFormatService.validate([file], format.identifier, [])
    def auth = getDetailsForLoggedInUser()
    String principal = auth.principal
    def model = mtc.newInstance(submitter: principal,
            submissionDate: new Date(), format: formatCommand)

    // generate list of RFTCs
    def files = [modelWrapper]
    def fileTrack = []
    fileTrack.addAll(expectedFiles.keySet())
    additional.each { addFile ->
        def pattern = expectedFiles.keySet().find {testPattern ->
            Pattern.matches(testPattern, addFile.getName())
        }
        if (pattern) {
            fileTrack.remove(pattern)
                String path = addFile.absolutePath
                boolean hidden = false
                if (pattern.contains("_manual")) {
                    hidden = true
                }
            files.push(rftc.newInstance(path: path, description: expectedFiles.get(pattern),
                mainFile: false, userSubmitted: false, hidden: hidden))
        }
    }
    if (fileTrack.isEmpty()) {
        String errorMessage = "Could not find some expected files for ${it}: ${fileTrack}"
        error(errorMessage)
        addModelError(file.name, errorMessage)
    }

    def revision = rtc.newInstance(model: model, files: files, format: formatCommand,
            validated: isValid, name: MODEL_NAME, description: DESCRIPTION,
            validationLevel: ValidationState.APPROVED, comment: "${comment}${MODEL_NAME}")
    return [files: files, revision: revision]
}

getFilesFromSubmissionData = { submissionData -> submissionData['files'] }

// converts a length of time into a formatted string
prettify = { time ->
    if (time < 0) {
        error("Expected a non-negative time value, not $time.")
        return
    }
    final int SECOND = 1
    final int MINUTE = 60 * SECOND
    final int HOUR = 60 * MINUTE
    final int DAY = 24 * HOUR
    /*
     * set the initial capacity to the maximum possible value
     * assumming double-digit figures for days, hours, minutes and seconds.
     */
    final StringBuilder result = new StringBuilder(38)
    // preserve original value for exception logging purposes
    def origTime = time
    try {
    if (time > DAY) {
        final int count = (int) (time / DAY)
        time = ((int) time) % DAY
        result.append("$count days ")
    }
    if (time > HOUR) {
        final int count = (int) (time / HOUR)
        time = ((int) time) % HOUR
        result.append("$count hours ")
    }
    if (time > MINUTE) {
        final int count = (int) (time / MINUTE)
        time = ((int) time) % MINUTE
        result.append("$count minutes ")
    }
    result.append("$time seconds")
    return result.toString()
    } catch(Exception e) {
        error("Failed to pretty-print duration $origTime")
    }
    return origTime
}

/*
 * Returns a (Jummp) user, appropriate for the biomodels model send as an
 * argument.
 */
getUser = { modelId, branch ->
    if (!branch || !modelId) {
        return null
    }
    // get user from appropriate biomodels table
    int submitterId = getSubmitterIdForModel(modelId, branch)
    if (!submitterId) {
        error "No submitter was found for model $modelId in the $branch branch."
        return null
    }
    def row = authConnection.firstRow("select * from auth_persons where person_id = ?",
            [submitterId])
    if (!row || !row?.email) {
        error("Could not find user information for submitter #$submitterId ($modelId): $row")
        return null
    }
    String email = row.email
    // if user does not exist, create it based on data available in biomodels
    def user
    userCacheModifier.lock()
    try {
        user = findUserByEmail(email)
        if (!user) {
            String personName = "${row.given_name} ${row.family_name}"
            String institution = row.organisation
            String un = getUsername(row)
            user = createUser(personName, institution, email, un)
            putInUserCache(email, user.id)
        }
        return user
    } finally {
        userCacheModifier.unlock()
    }
}

/**
 * Adds an entry to userCache.
 *
 * Logs an error if there is already an entry with the same key.
 *
 * @param key the email adress of the user
 * @param value the ID of the user
 */
putInUserCache = { key, value ->
    userCacheModifier.lock()
    try {
        def existing = userCache.putIfAbsent(key, value)
        if (existing) {
            error "UserCache already had an entry for email $key: user #$existing, not #$value"
        }
    } finally {
        userCacheModifier.unlock()
    }
}

/**
 * Resets the passwordExpired field for a given user.
 *
 * This unit of work is executed in a dedicated Session and transaction
 * so that other worker threads can see the new value.
 *
 * @param user the User instance for which to disable the passwordExpired field.
 * @return the given user with a valid password.
 */
unexpirePasswordIfNecessary = { user ->
    if (!user.passwordExpired) {
        return user
    }
    user.discard() // detach from current session
    User.withNewSession {
        user.attach() // so that it can be attached to the new one
        User.withTransaction {
            user.passwordExpired = false
            user.save(flush: true)
        }
    }
    assert !(user.passwordExpired)
    // now re-attach to the original session
    return user.attach()
}

/**
 * Looks up a user based on a given email address.
 *
 * userCache is consulted for potential matches first, falling back to a
 * dynamic finder call. If the latter returns a hit, it is added to userCache.
 *
 * If a user is found for the given email address, this method will clear the
 * passwordExpired flag if set.
 *
 * This method relies on the synchronisation barrier userCacheModifier to avoid
 * cache stampedes in cases when multiple threads request the same user which is
 * not present in the cache.
 */
findUserByEmail = { email ->
    assert email
    // guard against concurrent attempts to insert the same user
    userCacheModifier.lock()
    def result = null
    try {
        if (userCache.containsKey(email)) {
            long id = userCache.get(email)
            result = User.get(id)
        } else {
            result = User.findByEmail(email)
            if (result) {
                putInUserCache(email, result.id)
                unexpirePasswordIfNecessary(result)
            }
        }
        return result
    } finally {
        userCacheModifier.unlock()
    }
}

/**
 * Creates and persists a User instance based on the supplied information.
 *
 * @param name the name of the person for which this account is created
 * @param institution the person's affiliation, if known.
 * @param email the email of the user
 * @param un the account username
 */
createUser = { name, institution, email, un ->
    assert email
    def person = Person.newInstance(userRealName: name, institution: institution)
    user = User.newInstance(person: person, username: un, password: "autocreated",
            email: email, accountExpired: false, accountLocked: false,
            passwordExpired: false, enabled: true)
    if (!user.validate()) {
        def e = user.errors.allErrors.inspect()
        error "Cannot create valid account for submitter #$submitterId of model $modelId: $e"
        return null
    }
    long userId = userService.register(user, true)
    User.get(userId)
}

/**
 * Finds the user_id of the person that submitted a model.
 */
getSubmitterIdForModel = { String modelId, String branch ->
    def model = getModelById(modelId, branch)
    model?.submitter_id
}

publishModelRevision = { modelId, revision ->
    authenticate(username, password)
    try {
        aclUtilService.addPermission(revision, "ROLE_USER", BasePermission.READ)
        aclUtilService.addPermission(revision, "ROLE_ANONYMOUS", BasePermission.READ)
        revision.state = ModelState.PUBLISHED
        assert revision.save()
        addModelMsg modelId, "Successfully published $revision"
    } catch (Exception e) {
        addModelError(modelId, "Unable to publish revision ${revision.id} -- $e")
    } finally {
        logOut()
    }
}

/**
 * Convenience method for retrieving a model based on its identifier.
 *
 * Helps us deal with the fact that different tables have different column
 * names for the model identifier.
 */
getModelById = { modelId, branch ->
    String idColumnName = 'auto_gen_models' == branch ? 'id' : 'model_id'
    biomodelsConnection.firstRow("select * from $branch where $idColumnName = ?", [modelId])
}

authenticateAsUser = { user ->
    authenticate(user.username, "autocreated")
}

addPublicationLinkProvider =  { def cmd ->
    def publinkType = LinkType.valueOf(cmd.linkType)
    if (!PublicationLinkProvider.findByLinkType(publinkType)) {
        def provider = PublicationLinkProvider.newInstance(linkType: publinkType,
                pattern:cmd.pattern, identifiersPrefix: cmd.identifiersPrefix)
        provider.save(flush: true)
    }
}

// add supported publication link providers: DOI, PubMed, URI,...
populatePublicationLinkProviders = {
    addPublicationLinkProvider(plptc.newInstance(
            linkType: LinkType.PUBMED,
            pattern: "^\\d+",
            identifiersPrefix: "http://identifiers.org/pubmed/"))

    addPublicationLinkProvider(plptc.newInstance(
            linkType: LinkType.DOI,
            pattern: "^(doi\\:)?\\d{2}\\.\\d{4}.*",
            identifiersPrefix: "http://identifiers.org/doi/"))

    addPublicationLinkProvider(plptc.newInstance(
            linkType: LinkType.MANUAL_ENTRY,
            pattern: "\\A\\z" /* i.e. start of input then end of input -- ignored */))

    addPublicationLinkProvider(plptc.newInstance(
            linkType: LinkType.CUSTOM,
            pattern: "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"))
}

/**
 * Processes model of the month for a given model. The model of the month can
 * be comprised of several models, therefore the ModelOfTheMonth.models collection
 * is updated. The importer relies on an assumption that only one model of the month
 * can be published in a given month, which is valid given current data.
 */
processModelOfTheMonth = { model ->
    def modelId = model.publicationId ?: model.submissionId
    def dateFormatter = new java.text.SimpleDateFormat('yyyy-MM')
    String query = "select * from model_of_month where models_id = ?"
    biomodelsConnection.eachRow(query, [modelId]) { row ->
        def datePublished = dateFormatter.parse(row.pub_month)
        // see if there is an existing model of the month in the Jummp DB for
        // the given month
        def modelMonth = ModelOfTheMonth.findByPublicationDate(datePublished)
        if (!modelMonth) { //import new model of the month
            modelMonth = ModelOfTheMonth.newInstance(title: row.title,
                    authors: row.authors, publicationDate: datePublished)
            modelMonth.save() // save once to set the last_updated, then modify it
            modelMonth.lastUpdated=row.last_modification_date
        }
        modelMonth.addToModels(model)
        modelMonth.save()
    }
}


/*
 * Gets the username associated with a person in the biomodels database. Used to
 * to create JUMMP logins with the same IDs
 */
getUsername = { personRow ->
    def userInfo = authConnection.firstRow(
            "select * from auth_users where person_id = ?", [personRow.person_id])
    if (userInfo) {
        return userInfo.login
    }
    return personRow.email
}

/*
 * Searches the tables in biomodels to find the table containing given model
 */
getBranch = { modelId ->
    return bioModelsBranches.find {
        getModelById(modelId, it)
    }
}

createBMAnnotation = { revision, object, qual, creator ->
    def resourceRef = ResourceReference.findByUriAndDatatype(object, "biomodelsCustomAnnotation")
    if (!resourceRef) {
        resourceRef = ResourceReference.newInstance(uri: object, datatype: "biomodelsCustomAnnotation")
        resourceRef.save(failOnError: true)
    }
    def qualifier = Qualifier.findByQualifierTypeAndUri("biomodelsCustomAnnotation", qual)
    if (!qualifier) {
        qualifier = Qualifier.newInstance(qualifierType: "biomodelsCustomAnnotation",
                                          uri: qual)
        qualifier.save(failOnError:true)
    }
    def statement = Statement.newInstance(subjectId: 'modelLevelAnnotation',
            qualifier: qualifier, object: resourceRef)
    def modelElementType = ModelElementType.findByModelFormatAndName(revision.format, 'model')
    def elementAnnotation = ElementAnnotation.newInstance(creatorId: creator,
            statement: statement, revision: revision, modelElementType: modelElementType)
    elementAnnotation.save(failOnError:true)
}

getPublicationLink = { publication_id, publication_id_type ->
    if (!publication_id) {
        return null
    }
    switch(publication_id_type) {
        case 0: return "http://identifiers.org/pubmed/${publication_id}"
        case 1: return "http://identifiers.org/doi/${publication_id}"
    }
    return publication_id
}

/*
 * Gets the model details from BioModels DB
 */
getModelDetails = { modelId, modelBranch ->
    def modelDetails = [:]
    try {
        def row = getModelById(modelId, modelBranch)
        modelDetails['submissionDate'] = row.submission_date
        modelDetails['lastModified'] = row.last_modification_date
        modelDetails['publicationDate'] = row.publication_date
        modelDetails['originalModel'] = row.original_model
        if ("auto_gen_models" == modelBranch) {
            modelDetails['model_id'] = row.id
        } else {
            if ("publ" == modelBranch || "anno" == modelBranch) {
                modelDetails['jwsLink'] = row.jws_online
                final String biomodelsId = row.model_id
                final String submissionId = getSubmissionIdForBioModelsId biomodelsId
                if (!submissionId) {
                    throw new IllegalStateException("No submission identifier for $biomodelsId".toString())
                }
                modelDetails['model_id'] = submissionId
                modelDetails['biomodels_id'] = biomodelsId
            } else {
                modelDetails['model_id'] = row.model_id
            }
        }
        modelDetails['publication_id'] = row.publication_id
        modelDetails['publication_id_type'] = row.publication_id_type
    } catch(Exception e) {
        addModelError modelId, "Problem finding model details in branch $modelBranch: $e"
        return null
    }
    return modelDetails
}

getSubmissionIdForBioModelsId = { biomd ->
    assert biomd
    def result = biomodelsConnection.firstRow("select model_id from cura where biomodels_id = ?", [biomd])
    result?.model_id
}

setDefaultTarget(main)

