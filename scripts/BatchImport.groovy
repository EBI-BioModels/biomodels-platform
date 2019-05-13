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

target(main: "Puts everything together to import models from a given folder") {
    depends(configureProxy, enableExpandoMetaClass, packageApp, classpath, loadApp, configureApp)

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
    def session = appCtx.sessionFactory.openSession()
    TransactionSynchronizationManager.bindResource(appCtx.sessionFactory, new SessionHolder(session))

    int authIssues = authenticate()
    if (authIssues) {
        error "Wrong auth credentials. Why don't you try again?", authIssues
    }

    def mtc = grailsApp.classLoader.loadClass(
            "net.biomodels.jummp.core.model.ModelTransportCommand")
    def rftc = grailsApp.classLoader.loadClass(
            "net.biomodels.jummp.core.model.RepositoryFileTransportCommand")
    def model
    def decorator = grailsApp.classLoader.loadClass(
        "net.biomodels.jummp.core.model.identifier.decorator.AbstractAppendingDecorator")
    def mftc = grailsApp.classLoader.loadClass(
            "net.biomodels.jummp.core.model.ModelFormatTransportCommand")
    def mf = grailsApp.classLoader.loadClass(
            "net.biomodels.jummp.model.ModelFormat")
    def rtc = grailsApp.classLoader.loadClass(
            "net.biomodels.jummp.core.model.RevisionTransportCommand")
    def formatCommand = mftc.newInstance(identifier: "SBML", formatVersion: "*")
    def format = mf.findByIdentifierAndFormatVersion(formatCommand.identifier,
            formatCommand.formatVersion)

    rtc.context = appCtx
    def modelService = appCtx.modelService
    def modelFileFormatService = appCtx.modelFileFormatService

    def symlinkPattern = ~/[A-Z0-9]*\.xml/
    def targetPattern = ~/[a-zA-Z_\-\/0-9]*_url\.xml/
    // keep track of the number of models that are processed
    long processedCount = 0
    def failures = []
    long duration = System.currentTimeMillis()
    try {
        modelFolder.eachFileRecurse {
            boolean modelFileDetected = it.isFile() && symlinkPattern.matcher(it.name).matches()
            if (modelFileDetected) {
                try {
                    ++processedCount
                    log("Importing model file ${it.absolutePath}...")
                    boolean conventionFollowed = targetPattern.matcher(it.canonicalPath).matches()
                    if (!conventionFollowed) {
                        error "${it.absolutePath} should have been a symbolic link!"
                    }
                    final String MODEL_NAME = modelFileFormatService.extractName([it], format)
                    final String DESCRIPTION = modelFileFormatService.extractDescription([it], format)
                    boolean isValid = modelFileFormatService.validate([it], formatCommand.identifier, [])
                    model = mtc.newInstance(submitter: userAuthenticationDetails.principal,
                    submissionDate: new Date(), format: formatCommand)
                    def modelWrapper = rftc.newInstance(path: it.absolutePath, description: "$MODEL_NAME",
                    mainFile: true, userSubmitted: true, hidden: false)
                    def files = [modelWrapper]
                    def revision = rtc.newInstance(model: model, files: files, format: formatCommand,
                            validated: isValid, name: MODEL_NAME, description: DESCRIPTION,
                            comment: "Import of $MODEL_NAME".toString())
                    def result = modelService.uploadValidatedModel(files, revision)
                    if (!result) {
                        failures.add(it.absolutePath)
                    }
                    log("...finished importing model file ${it.absolutePath}")
                } catch (Throwable t) {
                    error("Something went wrong with ${it.name} - ${t.message}")
                    t.printStackTrace()
                }
            }
        }
    } finally {
        duration = (System.currentTimeMillis() - duration) / 1000
        String formattedDuration = prettify(duration)
        log("Imported $processedCount models (${failures.size()} failures) in $formattedDuration")
        if (failures) {
            log("Failed to import the following models:\n${failures.join('\n')}")
        }
        def camelContext = appCtx.camelContext
        duration = System.currentTimeMillis()
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

    (username, password) = [c.'username', c.'password']
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

target(authenticate: "Attempts to authenticate the user or fails badly") {
    def authToken = new UsernamePasswordAuthenticationToken(username, password)
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

setDefaultTarget(main)
