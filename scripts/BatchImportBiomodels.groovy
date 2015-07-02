/**
* Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

import grails.converters.*
import org.apache.commons.io.FileUtils
import org.codehaus.groovy.grails.web.json.*
import org.springframework.orm.hibernate3.SessionFactoryUtils
import org.springframework.orm.hibernate3.SessionHolder
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import net.biomodels.jummp.annotationstore.ResourceReference
import grails.util.Holders
import groovy.sql.Sql

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
* Location of simulation files
*/
File simulationFolder

/*
* List of users used in this import
*/
def usersUsed = [] as Set

target(main: "Puts everything together to import models from a given folder") {
    bootstrapOnce()
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
    // bind a Hibernate Session to avoid lazy initialization exceptions
    TransactionSynchronizationManager.bindResource(appCtx.sessionFactory,
        new SessionHolder(SessionFactoryUtils.getSession(appCtx.sessionFactory, true)))

    int authIssues = authenticate(username, password)
    if (authIssues) {
        error "Wrong auth credentials. Why don't you try again?", authIssues
    }

    def mtc = grailsApp.classLoader.loadClass(
            "net.biomodels.jummp.core.model.ModelTransportCommand")
    def User = grailsApp.classLoader.loadClass(
            "net.biomodels.jummp.plugins.security.User")
    def Person = grailsApp.classLoader.loadClass(
            "net.biomodels.jummp.plugins.security.Person")
    def rftc = grailsApp.classLoader.loadClass(
            "net.biomodels.jummp.core.model.RepositoryFileTransportCommand")
    def rtc = grailsApp.classLoader.loadClass(
            "net.biomodels.jummp.core.model.RevisionTransportCommand")
    def model
    def decorator = grailsApp.classLoader.loadClass(
        "net.biomodels.jummp.core.model.identifier.decorator.AbstractAppendingDecorator")
    def mftc = grailsApp.classLoader.loadClass(
            "net.biomodels.jummp.core.model.ModelFormatTransportCommand")
    def mf = grailsApp.classLoader.loadClass(
            "net.biomodels.jummp.model.ModelFormat")
    def domainadapter = grailsApp.classLoader.loadClass(
            "net.biomodels.jummp.core.adapters.DomainAdapter")
            
    def Revision = grailsApp.classLoader.loadClass(
            "net.biomodels.jummp.model.Revision")
            
    def Model = grailsApp.classLoader.loadClass(
            "net.biomodels.jummp.model.Model")
            
    def CurationNotes = grailsApp.classLoader.loadClass(
            "net.biomodels.jummp.deployment.biomodels.CurationNotes")
            
    def ModelOfTheMonth = grailsApp.classLoader.loadClass(
            "net.biomodels.jummp.deployment.biomodels.ModelOfTheMonth")
            
    def ResourceReference = grailsApp.classLoader.loadClass(
            "net.biomodels.jummp.annotationstore.ResourceReference")
    
    def Statement = grailsApp.classLoader.loadClass(
            "net.biomodels.jummp.annotationstore.Statement")
            
    def ElementAnnotation = grailsApp.classLoader.loadClass(
            "net.biomodels.jummp.annotationstore.ElementAnnotation")

    def Qualifier = grailsApp.classLoader.loadClass(
            "net.biomodels.jummp.annotationstore.Qualifier")
            
    decorator.context = appCtx
    rtc.context = appCtx
    def modelService = appCtx.modelService
    def modelFileFormatService = appCtx.modelFileFormatService
    def userService = appCtx.userService
    def springSecurityService = appCtx.springSecurityService
    
    def symlinkPattern = ~/[A-Z0-9]*\.xml/
    def targetPattern = ~/[a-zA-Z_\-\/0-9]*_url\.xml/
    // keep track of the number of models that are processed
    long processedCount = 0
    def failures = []
    
    /*
    * Issue: Domain class constraints werent being applied, leading to the 
    * familiar issue of mime types not being set. Fixed by applying them
    * as below.
    */
    
    def domainClassGrailsPlugin = grailsApp.classLoader.loadClass(
                "org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin")
    grailsApp.domainClasses.each { gc ->
        domainClassGrailsPlugin.addValidationMethods(grailsApp,
                                                     gc,
                                                     grailsApp.mainContext)
    }
    
    /*
    * Instantiate direct connections to DB
    */
    def biomodelsConnection = Sql.newInstance("jdbc:mysql://${bmServer}:${bmPort}/${bmDB}", bmUsername,
                              bmPassword, "com.mysql.jdbc.Driver")
    
    def authConnection = Sql.newInstance("jdbc:mysql://${authServer}:${authPort}/${authDB}", authUsername,
                              authPassword, "com.mysql.jdbc.Driver")
                          
                              
    long duration = System.currentTimeMillis()
    try {
        modelFolder.eachFileRecurse {
            boolean modelFileDetected = it.isFile() && symlinkPattern.matcher(it.name).matches()
            if (modelFileDetected) {
                try {
                    ++processedCount
                    String modelId = it.getName().replace(".xml", "")
                    // Creates/retrieves user based on the user associated with
                    // the model in the biomodels DB
                    String modelBranch = getBranch(modelId, biomodelsConnection)
                    def user = getUser(modelId, biomodelsConnection, 
                                        authConnection, userService,
                                        User, Person, modelBranch)
                    if (user) {
                        authenticateAsUser(user, springSecurityService)
                        def modelDetails = getModelDetails(modelId, modelBranch, biomodelsConnection)
                        def additionalFiles = []
                        File originalFile = null
                        File parent = new File(it.getParent())
                        //set additional files / original file
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
                            def initialSubmission = getSubmissionData(originalFile, 
                                                              additionalFiles - originalFile,
                                                              "Original import of ",
                                                              rftc,
                                                              rtc,
                                                              mtc,
                                                              mf,
                                                              modelFileFormatService)
                            def firstModel = modelService.uploadValidatedModel(initialSubmission[0], initialSubmission[1])
                            if (!firstModel) {
                                log("...could not import initial file: ${originalFile.absolutePath}")
                                failures.add(it.absolutePath)
                            }
                            else {
                                processModelOfTheMonth(firstModel, biomodelsConnection, ModelOfTheMonth)
                                saveCurationNotes(firstModel, biomodelsConnection, 
                                                  CurationNotes, simulationFolder,
                                                  authConnection, User, Person,
                                                  userService)
                                firstModel.firstPublished = modelDetails["publicationDate"]
                                firstModel.submissionId = modelDetails["model_id"]
                                Revision.executeUpdate("update Revision set uploadDate = :newDate where model = :modelImported", [newDate:modelDetails["submissionDate"], modelImported: firstModel])
                                Model.executeUpdate("update Model set submissionId = :newId where id = :modelId", [newId:modelDetails["model_id"], modelId: firstModel.id])
                                def secondRevision = getSubmissionData(it,
                                                                   additionalFiles,
                                                                   "Current version of ",
                                                                   rftc,
                                                                   rtc,
                                                                   mtc,
                                                                   mf,
                                                                   modelFileFormatService)
                                //update the RTC generated by above call to the model returned 
                                // from submitting the original file.
                                secondRevision[1].model = domainadapter.getAdapter(firstModel).toCommandObject()
                                def secondResult = modelService.addValidatedRevision(secondRevision[0], [], secondRevision[1])
                                if (!secondResult) {
                                    log("...could not update to latest version: ${originalFile.absolutePath}")
                                    failures.add(it.absolutePath)
                                }
                                boolean curated = "publ" == modelBranch
                                createBMAnnotation(secondResult, curated, "curated", 
                                                   user.person.userRealName, 
                                                   ResourceReference, Qualifier, 
                                                   Statement, ElementAnnotation)
                               String publicationLink = getPublicationLink(modelDetails["publication_id"],
                                                                           modelDetails["publication_id_type"])
                               if (publicationLink) {
                                   createBMAnnotation(secondResult, publicationLink, "originalModel", 
                                                   user.person.userRealName, 
                                                   ResourceReference, Qualifier, 
                                                   Statement, ElementAnnotation)
                               }
                               
                                if (modelDetails["jwsLink"]) {
                                   createBMAnnotation(secondResult, modelDetails["jwsLink"], 
                                                      "onlineSimulation", user.person.userRealName, 
                                                      ResourceReference, Qualifier, 
                                                      Statement, ElementAnnotation)
                                }
                               
                                secondResult.uploadDate = modelDetails["lastModified"]
                                secondResult.save()
                            }
                            log("...finished importing model file ${it.absolutePath}")
                        }
                        else {
                            error "No original file for ${it.absolutePath}"
                        }
                        log("...finished importing model file ${it.absolutePath}")
                    }
                    else {
                        error("No user found for ${it.absolutePath}")
                        failures.add(it.absolutePath)
                    }
                    } catch (Throwable t) {
                        error("Something went wrong with ${it.name} - ${t.message}")
                        t.printStackTrace()
                    }
                    authenticate(username, password)
            }
        }
    } finally {
        duration = (System.currentTimeMillis() - duration) / 1000
        String formattedDuration = prettify(duration)
        log("Imported $processedCount models (${failures.size()} failures) in $formattedDuration")
        if (failures) {
            log("Failed to import the following models:\n${failures.join('\n')}")
        }
        usersUsed.each { userToExpire ->
            userService.expirePassword(userToExpire.id, true)
        }
        def camelContext = appCtx.camelContext
        duration = (System.currentTimeMillis() - duration) / 1000
        camelContext.shutdown()
        duration = (System.currentTimeMillis() - duration) / 1000
        log("Waited ${prettify(duration)} for Camel to stop gracefully.")
    }
    return 0
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
    (bmServer, bmPort, bmDB, bmUsername, bmPassword) = [c.'biomodelsServer',
                                                        c.'biomodelsPort',
                                                        c.'biomodelsDB',
                                                        c.'biomodelsUsername',
                                                        c.'biomodelsPassword']
                                                        
    (authServer, authPort, authDB, authUsername, authPassword) = [c.'authServer',
                                                                  c.'authPort',
                                                                  c.'authDB',
                                                                  c.'authUsername',
                                                                  c.'authPassword']
                                                        
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
*  Generates data structures needed for submission. Returns an array, with
* the first being the list of RFTCs needed to submit the model and the second
* being the revision transport command. 
*/
getSubmissionData = { file, additional, comment, rftc, rtc, mtc, mf, modelFileFormatService -> 
    def modelWrapper = rftc.newInstance(path: file.absolutePath, description: "",
                                    mainFile: true, userSubmitted: true, hidden: false)
                    
    def formatCommand = modelFileFormatService.inferModelFormat([modelWrapper])
    def format = mf.findByIdentifierAndFormatVersion(formatCommand.identifier,
                                                     formatCommand.formatVersion)
    final String MODEL_NAME = modelFileFormatService.extractName([file], format)?: new File(file.absolutePath).getName()
    modelWrapper.description = "${MODEL_NAME}"
    final String DESCRIPTION = modelFileFormatService.extractDescription([file], format)
    boolean isValid = modelFileFormatService.validate([file], format.identifier, [])
    model = mtc.newInstance(submitter: userAuthenticationDetails.principal,
                            submissionDate: new Date() /*adjust from DB*/, format: formatCommand)
    def files = [modelWrapper]
    additional.each { addFile ->
           files.push(rftc.newInstance(path: addFile.absolutePath, description: "TODO",
                      mainFile: false, userSubmitted: true /*todo*/, hidden: false))
    }
    return [files, rtc.newInstance(model: model, files: files, format: formatCommand,
                            validated: isValid, name: MODEL_NAME, description: DESCRIPTION,
                            comment: comment+MODEL_NAME)] as Object[]
}

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
* Returns a (Jummp) user, appropirate for the biomodels model send as an
* argument.
*/
getUser = { modelId, biomodelsConnection, authConnection, userService, User, Person, branch ->
    if (branch) {
        // get user from appropriate biomodels table
        int submitterId = biomodelsConnection.firstRow("select submitter_id from "+branch+" where model_id='"+modelId+"'").submitter_id
        def row = authConnection.firstRow("select * from auth_persons where person_id="+submitterId)
        String email = row.email
        // if user does not exist, create it based on data available in biomodels
        def user = User.findByEmail(email)
        if (!user) {
            def person = Person.newInstance(userRealName: row.given_name+" "+row.family_name,
                                            institution: row.organisation)
            user = User.newInstance(person: person,
                                    username: getUsername(row, authConnection),
                                    password: "autocreated",
                                    email: email 
                                    )
            long userId = userService.register(user, true)
            if (userId) {
                user = User.get(userId)
            }
            else {
                user = null
            }
        }
        else {
            // otherwise use existing user, after un-expiring their password
            userService.expirePassword(user.id, false)
        }
        usersUsed.add(user)
        return user
    }
    return null
}

authenticateAsUser = { user, springSecurityService ->
    authenticate(user.username, "autocreated")
}

processModelOfTheMonth = { model, sql, ModelOfTheMonth ->
    def dateFormatter = new java.text.SimpleDateFormat('yyyy-MM')
    sql.eachRow("select * from model_of_month where models_id LIKE '%{"+model.id+"}%'") { row ->
        def datePublished = dateFormatter.parse(row.pub_month)
        def modelMonth = ModelOfTheMonth.findByPublicationDate(datePublished)
        if (!modelMonth) {
            modelMonth = ModelOfTheMonth.newInstance(title: row.title,
                                                     authors: row.authors,
                                                     publicationDate: datePublished,
                                                     lastUpdated: row.last_modification_date)
        }
        modelMonth.addToModels(model)
        modelMonth.save()
    }
}

saveCurationNotes = { model, sql, CurationNotes, imageFolder, authConnection, User, Person, userService ->
    def row = sql.firstRow("select * from simulations, cura where simulations.curation_id = cura.model_id and (cura.model_id = '"+model.id+"' OR cura.biomodels_id = '"+model.id+"')")
    if (row) {
        def submitter = getUserFromBiomodelsId(row.submitter_id, authConnection, User, Person, userService)
        def modifier = submitter
        if (row.submitter_id != row.last_modifier_id) {
            modifier = getUserFromBiomodelsId(row.last_modifier_id, authConnection, User, Person, userService)
        }
        def notes = CurationNotes.newInstance(model: model,
                                              submitter: submitter,
                                              lastModifier: modifier,
                                              dateAdded: row.submission_date,
                                              lastModified: row.last_modification_date,
                                              comment: row.comments,
                                              curationImage: new File(imageFolder, row.file_name).getBytes())
        notes.save(failOnError: true)
    }
}

getUserFromBiomodelsId = { bmPersonId, sql, User, Person ->
    def personDetails = row.firstRow("select * from auth_persons where person_id = "+bmPersonId)
    def existing = Person.findByEmail(row.email)
    if (existing) {
        return User.findByPerson(existing)
    }
    def person = Person.newInstance(userRealName: row.given_name+" "+row.family_name,
                                    institution: row.organisation)
    def user = User.newInstance(person: person,
                                username: getUsername(row, sql),
                                password: "autocreated",
                                email: email 
                               )
    long userId = userService.register(user, true)
    if (userId) {
        return User.get(userId)
     }
}


/*
* Gets the username associated with a person in the biomodels database. Used to
* to create JUMMP logins with the same IDs
*/
getUsername = { personRow, sql ->
    def userInfo = sql.firstRow("select * from auth_users where person_id = "+personRow.person_id)
    if (userInfo) {
        return userInfo.login
    }
    return personRow.email
}

/*
* Searches the tables in biomodels to find the table containing given model
*/
getBranch = { modelId, sql ->
    def branches = ["publ", "uncura_publ", "anno", "uncura_anno", "cura"]
    return branches.find {
        testBranch(modelId, it, sql)
    }
}

createBMAnnotation = { revision, object, qual, creator, ResourceReference, Qualifier, Statement, ElementAnnotation ->
    def resourceRef = ResourceReference.newInstance(uri: object)
    resourceRef.save(failOnError: true)
    def qualifier = Qualifier.newInstance(qualifierType: "biomodelsCustomAnnotation",
                                          uri: qual)
    qualifier.save(failOnError:true)
    def statement = Statement.newInstance(subjectId: 'modelLevelAnnotation',
                                          qualifier: qualifier,
                                          object: resourceRef)
    statement.save(failOnError:true)
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
        case 0: return "http://identifiers.org/pubmed/" + publication_id
        case 1: return "http://identifiers.org/doi/"+publication_id
    }
    return publication_id
}

/*
* Gets the model details from biomodelsDB 
*/
getModelDetails = { modelId, modelBranch, sql ->
    def modelDetails = [:]
    try {
        def row = sql.firstRow("select * from "+modelBranch+" where model_id='"+modelId+"'")
        modelDetails['submissionDate'] = row.submission_date;
        modelDetails['lastModified'] = row.last_modification_date;
        modelDetails['publicationDate'] = row.publication_date;
        modelDetails['originalModel'] = row.original_model;
        modelDetails['jwsLink'] = row.jws_online;
        modelDetails['model_id'] = row.model_id;
        modelDetails['publication_id'] = row.publication_id;
        modelDetails['publication_id_type'] = row.publication_id_type;
    }
    catch(Exception e) {
        e.printStackTrace()
        System.out.println("OFFENDING MODEL: "+modelId)
        System.exit(0);
    }
    return modelDetails
}

testBranch = { modelId, branch, sql -> 
    return sql.firstRow("SELECT model_id FROM "+branch+" where model_id='"+modelId+"'") !=null 
}

setDefaultTarget(main)
