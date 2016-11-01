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
 *
 * Additional permission under GNU Affero GPL version 3 section 7
 *
 * If you modify Jummp, or any covered work, by linking or combining it with
 * groovy, Apache Commons, Spring Framework, Grails (or a modified version of that library), containing parts
 * covered by the terms of Apache License v2.0, the licensors of this
 * Program grant you additional permission to convey the resulting work.
 * {Corresponding Source for a non-source form of such a combination shall
 * include the source code for the parts of groovy, Apache Commons, Spring Framework, Grails used as well as
 * that of the covered work.}
 **/

import grails.converters.JSON
import groovy.sql.Sql
import java.util.regex.Pattern
import net.biomodels.jummp.core.model.ValidationState
import org.springframework.orm.hibernate4.SessionHolder
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
 * The authentication details of the user that is logged in.
 */
def userAuthenticationDetails

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

/*
 * List of users used in this import
 */
def usersUsed = [] as Set

/**
 * The branches of BioModels where we look for model information.
 */
def bioModelsBranches = ["publ"]//, "uncura_publ", "anno", "uncura_anno", "cura", "auto_gen_models"]

/*
 * Domain classes that will be needed in multiple closures, declared globally,
 * instantiated in loadClasses()
 */
def User
def Person
def rftc
def mftc
def rtc
def mtc
def plptc
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
def PublicationLinkProvider
def LinkType

/**
 * The Hibernate SessionFactory
 */
def sessionFactory

/**
 * Services used by the script
 */
def modelService
def modelFileFormatService
def userService
def springSecurityService

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
    def result
    try {
        def currentAuth = SecurityContextHolder.context.authentication
        SecurityContextHolder.context.authentication = auth
        result = closure.call()
    } finally {
        SecurityContextHolder.context.authentication = currentAuth
        return result
    }
}

registerUser = { user ->
    simpleRunAs( userAuthenticationDetails, {
        userService.register(user, true)
    })
}

/**
 * Extracts the curation notes corresponding to the model from BioModels that we are importing.
 */
setCurationNotes = { modelSubmitted ->
    def row = biomodelsConnection.firstRow("""\
SELECT *
FROM
    simulations
    join cura on simulations.curation_id = cura.model_id
WHERE
cura.model_id = :mid
OR cura.biomodels_id = :mid""", [mid: modelSubmitted.submissionId])
    log("curation notes for ${modelSubmitted.submissionId} => ${row?.entrySet()?.toString()}")
    if (row) {
        def submitter = getUserFromBiomodelsId(row.submitter_id)
        if (!submitter) {
            error("Could not find person with id: ${row.submitter_id}, curation notes not imported for ${modelSubmitted.submissionId}")
            return
        }
        def modifier = submitter
        if (row.submitter_id != row.last_modifier_id) {
             modifier = getUserFromBiomodelsId(row.last_modifier_id)
             if (!modifier) {
                 error("Could not find person with id: ${row.last_modifier_id}, curation notes not imported for ${modelSubmitted.submissionId}")
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

    // initialise settings, authenticate using supplied credentials
    prepareImporter()

    // load necessary classes
    loadClasses()

    // don't send registration confirmation emails to model submitters
    grailsApp.config.jummp.security.registration.email.send = false

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

    int authIssues = authenticate(username, password)
    if (authIssues) {
        error "Wrong auth credentials. Why don't you try again?", authIssues
    }
}

target(prepareDataSources: "initialisation of machinery for database interaction") {
    // bind a Hibernate Session to avoid lazy initialization exceptions
    def session = sessionFactory.openSession()
    TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session))

    // Instantiate direct connections to DB
    biomodelsConnection = Sql.newInstance("jdbc:mysql://${bmServer}:${bmPort}/${bmDB}",
            bmUsername, bmPassword, "com.mysql.jdbc.Driver")
    authConnection = Sql.newInstance("jdbc:mysql://${authServer}:${authPort}/${authDB}",
            authUsername, authPassword, "com.mysql.jdbc.Driver")
}

target(loadClasses: 'Loads required classes in the Jummp Grails environment') {
    // transport commands
    rftc = loadClass("net.biomodels.jummp.core.model.RepositoryFileTransportCommand")
    mftc = loadClass("net.biomodels.jummp.core.model.ModelFormatTransportCommand")
    rtc = loadClass("net.biomodels.jummp.core.model.RevisionTransportCommand")
    mtc = loadClass("net.biomodels.jummp.core.model.ModelTransportCommand")
    plptc = loadClass("net.biomodels.jummp.core.model.PublicationLinkProviderTransportCommand")

    // submission-related domain classes
    Person = loadClass("net.biomodels.jummp.plugins.security.Person")
    User = loadClass("net.biomodels.jummp.plugins.security.User")
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
    modelFileFormatService = appCtx.modelFileFormatService
    userService = appCtx.userService
    springSecurityService = appCtx.springSecurityService
}

// keep track of the number of models that are processed
long processedCount = 0
def failures = [:]
target(main: "Puts everything together to import models from a given folder") {
    bootstrapJummp()

    def symlinkPattern = ~/[A-Z0-9]*\.xml/
    def targetPattern = ~/[a-zA-Z_\-\.\/0-9]*_url\.xml/

    long duration = System.currentTimeMillis()
    try {
        modelFolder.eachFileRecurse {
            boolean modelFileDetected = it.isFile() && symlinkPattern.matcher(it.name).matches()
            String modelId = it.getName().replace(".xml", "")
            String modelBranch = getBranch(modelId)
            if (modelFileDetected && modelBranch) {
                try {
                    ++processedCount
                    // Creates/retrieves user based on the user associated with
                    // the model in the biomodels DB
                    def user = getUser(modelId, modelBranch)
                   // retrieves the model details stored in the biomodels DB
                   def modelDetails = getModelDetails(modelId, modelBranch)

                   if (user && modelDetails) {
                        //login as user submitting the model
                        authenticateAsUser(user)

                        //set additional files / original file
                        def additionalFiles = []
                        File originalFile = null
                        File parent = new File(it.getParent())
                        parent.eachFile  { additional ->
                            if (additional != it)  {
                                additionalFiles.push(additional)
                            }
                            if (additional.getName().contains("origin")) {
                                originalFile = additional
                            }
                        }
                        boolean conventionFollowed = targetPattern.matcher(it.canonicalPath).matches()
                        if (!conventionFollowed) {
                            error "${it.absolutePath} should have been a symbolic link!"
                        }
                        //if original file exists, only then proceed, otherwise there is an error
                        if (originalFile) {
                            //submit first revision, with original file as the main file
                            def additionals = additionalFiles - originalFile
                            def submission = getSubmissionData(originalFile, additionals,
                                    "Original import of ")
                            def files = getFilesFromSubmissionData(submission)
                            def revisionCmd = submission.get("revision")
                            def firstModel = modelService.uploadValidatedModel(files, revisionCmd)
                            if (!firstModel) {
                                log("...could not import initial file: ${originalFile.absolutePath}")
                                failures.put(it.name, "Error importing original file")
                            }
                            else {
                                if ("auto_gen_models" != modelBranch) {
                                    //Update model of the month
                                    processModelOfTheMonth(firstModel)
                                }

                                /* Add the curation notes */
                                setCurationNotes(firstModel)

                                // TODO should also create a publication for this model !!

                                /* Update revision / model details */
                                firstModel.revisions[0].uploadDate = modelDetails["submissionDate"]
                                firstModel.firstPublished = modelDetails["publicationDate"]
                                firstModel.submissionId = modelDetails["model_id"]
                                firstModel.validate()
                                //inspectSession()
                                firstModel.save(flush: true)

                                // now submit the model as stored in BioModels
                                def revisionData = getSubmissionData(it, additionalFiles,
                                        "Current version of ")
                                files = getFilesFromSubmissionData(revisionData)
                                def update = revisionData.get("revision")

                                String fmtName = update.format.identifier
                                String fmtVersion = update.format.formatVersion
                                // set the revision's model to be the one we just submitted
                                update.model = mtc.newInstance(
                                    format: mftc.newInstance(identifier: fmtName,
                                            formatVersion: fmtVersion),
                                    submitter: userAuthenticationDetails.principal,
                                    submissionDate: modelDetails["submissionDate"],
                                    submissionId: firstModel.submissionId
                                )

                                /* Upload second / final version of the model */
                                def secondResult = modelService.addValidatedRevision(files, [],
                                        update)
                                if (!secondResult) {
                                    log("...could not update to latest version: ${originalFile.absolutePath}")
                                    failures.add(it.absolutePath)
                                }
                                boolean curated = "publ" == modelBranch

                                /* Create annotations for the publication link / branch / jws etc */
                                createBMAnnotation(secondResult, curated, "curated", user.person.userRealName)
                                String publicationLink = getPublicationLink(modelDetails["publication_id"],
                                        modelDetails["publication_id_type"])
                                if (publicationLink) {
                                   createBMAnnotation(secondResult, publicationLink, "originalModel",
                                           user.person.userRealName)

                                }

                                if (modelDetails["jwsLink"]) {
                                   createBMAnnotation(secondResult, modelDetails["jwsLink"],
                                           "onlineSimulation", user.person.userRealName)
                                }

                                secondResult.uploadDate = modelDetails["lastModified"]
                                secondResult.save()
                            }
                            log("...finished importing model file ${it.absolutePath}")
                        }
                        else {
                            error "No original file for ${it.absolutePath}"
                            failures.put(it.name, "No original file found")
                        }
                        log("...finished importing model file ${it.absolutePath}")
                   } else {
                        if (!user) {
                            error("No user found for ${it.absolutePath}")
                            failures.put(it.name, "No user found, please check details in BioModels db")
                        } else {
                            error("No model details found for ${it.absolutePath}")
                            failures.put(it.name, "Error retrieving model details from BioModels db")
                        }
                   }
                // See http://docs.jboss.org/hibernate/orm/4.1/devguide/en-US/html/ch04.html#d5e971
                if (processedCount % 20 == 0) {
                   def session = sessionFactory.currentSession
                   session.flush()
                   session.clear()
                }
                } catch (Throwable t) {
                    error("Something went wrong with ${it.name} - ${t.message}")
                    failures.put(it.name, t.message)
                    t.printStackTrace()
                }
                //Log back in with the user supplied credentials
                authenticate(username, password)
            }
        }
    } finally {
        duration = (System.currentTimeMillis() - duration) / 1000
        String formattedDuration = prettify(duration)
        log("Imported $processedCount models (${failures.size()} failures) in $formattedDuration")
        if (failures) {
            log("Failed to import the following models:")
            failures.each { f ->
                log "${f.key}: ${f.value}"
            }
        }
        cleanup()
    }
    return 0
}

target(cleanup: "Shutdown hook used to gracefully close resources") {
    closeDataSources()
    closeCamel()
    expireUserPasswords()
}

target(expireUserPasswords: 'Forces users with accounts created herein to reset their passwords') {
        usersUsed.each { userToExpire ->
            userService.expirePassword(userToExpire.id, true)
        }
}

target(closeDataSources: "Clears transaction synchronisations and closes active Hibernate session") {
    def session = sessionFactory.currentSession
    if (!session) {
        error("No active session found for current thread -- skipping Hibernate cleanup.")
    }
    session.flush()
    session.clear()
    biomodelsConnection?.close()
    authConnection?.close()
    TransactionSynchronizationManager.unbindResourceIfPossible(sessionFactory)
}

target(closeCamel: "Shuts down the Camel instance, awaiting for current messages to be delivered") {
    def camelContext = appCtx.camelContext
    duration = System.currentTimeMillis()
    camelContext.shutdown()
    duration = (System.currentTimeMillis() - duration) / 1000
    //log("Waited ${prettify(duration)} for Camel to stop gracefully.")
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

inspectModel = { model ->
    log "model errors : ${model.hasErrors()} -- ${model.errors.allErrors}"
    long id = model.id
    if (!id) {
        log("Model $model has not been saved!")
    } else {
        def modelFromDb = Model.withNewSession { Model.get(id) }
        log("in the database: ${modelFromDb}")
    }
}

authenticate = { user, passwd ->
    def authToken = new UsernamePasswordAuthenticationToken(user, passwd)
    def auth = appCtx.getBean("authenticationManager").authenticate(authToken)
    if (!auth.authenticated) {
        error("Are you sure that is the right username/password combination for your account?",
                16)
    }
    SecurityContextHolder.getContext().setAuthentication(auth)
    userAuthenticationDetails = auth
    return 0
}

error = { String msg, int code = -1 ->
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
    def model = mtc.newInstance(submitter: userAuthenticationDetails.principal,
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
        failures.put(file.name, errorMessage)
    }

    def revision = rtc.newInstance(model: model, files: files, format: formatCommand,
            validated: isValid, name: MODEL_NAME, description: DESCRIPTION,
            validationLevel: ValidationState.APPROVED, comment: "${comment}${MODEL_NAME}")
    return [files: files, revision: revision]
}

getFilesFromSubmissionData = { submissionData -> submissionData['files'] }

prettify = { long time ->
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
    if (time > DAY) {
        final int count = time / DAY
        time = time % DAY
        result.append("$count days ")
    }
    if (time > HOUR) {
        final int count = time / HOUR
        time = time % HOUR
        result.append("$count hours ")
    }
    if (time > MINUTE) {
        final int count = time / MINUTE
        time = time % MINUTE
        result.append("$count minutes ")
    }
    result.append("$time seconds")
    return result.toString()
}

/*
 * Returns a (Jummp) user, appropriate for the biomodels model send as an
 * argument.
 */
getUser = { modelId, branch ->
    if (branch) {
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
        def user = User.findByEmail(email)
        if (!user) {
            String personName = "${row.given_name} ${row.family_name}"
            def person = Person.newInstance(userRealName: personName,
                    institution: row.organisation)
            user = User.newInstance(person: person,
                    username: getUsername(row), password: "autocreated",
                    email: email, accountExpired: false, accountLocked: false,
                    passwordExpired: false, enabled: true)
            if (!user.validate()) {
                def e = user.errors.allErrors.inspect()
                error "Cannot create valid account for submitter #$submitterId of model $modelId: $e"
                return null
            }
            long userId = userService.register(user, true)
            if (userId) {
                user = User.get(userId)
            } else {
                user = null
            }
        } else {
            // otherwise use existing user, after un-expiring their password
            userService.expirePassword(user.id, false)
        }
        usersUsed.add(user)
        return user
    }
    return null
}

/**
 * Finds the user_id of the person that submitted a model.
 */
getSubmitterIdForModel = { String modelId, String branch ->
    def model = getModelById(modelId, branch)
    model?.submitter_id
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

/*
* Processes model of the month for a given model. The model of the month can
* be comprised of several models, therefore the ModelOfTheMonth.models collection
* is updated. The importer relies on an assumption that only one model of the month
* can be published in a given month, which is valid given current data.
*/
processModelOfTheMonth = { model ->
    def dateFormatter = new java.text.SimpleDateFormat('yyyy-MM')
    String query = "select * from model_of_month where models_id LIKE '%${model.submissionId}%'"
    biomodelsConnection.eachRow(query) { row ->
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
                                          qualifier: qualifier,
                                          object: resourceRef)
    def elementAnnotation = ElementAnnotation.newInstance(creatorId: creator,
                                                          statement: statement,
                                                          revision: revision)
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
            }
            modelDetails['model_id'] = row.model_id
        }
        modelDetails['publication_id'] = row.publication_id
        modelDetails['publication_id_type'] = row.publication_id_type
    } catch(Exception e) {
        error("Problem finding model details for $modelId in branch $modelBranch. ${e.message}.")
        e.printStackTrace()
        return null
    }
    return modelDetails
}

setDefaultTarget(main)

