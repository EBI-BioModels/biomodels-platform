/**
 * Copyright (C) 2010-2020 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
 * Spring Framework, Spring Security (or a modified version of that library), containing parts
 * covered by the terms of Apache License v2.0, the licensors of this
 * Program grant you additional permission to convey the resulting work.
 * {Corresponding Source for a non-source form of such a combination shall
 * include the source code for the parts of Spring Framework, Spring Security used as well as
 * that of the covered work.}
 **/

package net.biomodels.jummp.webapp

import grails.async.Promises
import grails.converters.JSON
import grails.converters.XML
import grails.plugin.springsecurity.annotation.Secured
import net.biomodels.jummp.CommonController
import net.biomodels.jummp.core.model.CurationState
import net.biomodels.jummp.core.model.ModelFormatTransportCommand as MFTC
import net.biomodels.jummp.core.model.ModelTransportCommand as MTC
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand as RFTC
import net.biomodels.jummp.core.model.RevisionTransportCommand as RTC
import net.biomodels.jummp.core.model.ValidationState
import net.biomodels.jummp.core.util.JummpHttpService
import net.biomodels.jummp.utils.CollectionHelper
import net.biomodels.jummp.utils.FileHelper
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.codehaus.groovy.grails.web.json.JSONElement
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean

@Secured(['IS_AUTHENTICATED_FULLY'])
class SubmissionController extends CommonController implements InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(SubmissionController.class)
    static final String FILE_DESCRIPTION_MISSING = "The file needs a description"
    static final String FILE_NAME_INVALID = "The file name is invalid (use only letters, digits, spaces, dots, " +
        "hyphens, plus signs and underscores, and end it with a file extension)"
    def fileSystemService
    def grailsApplication
    def groovyPageRenderer
    def mailingService
    def messageSource
    def modelFileFormatService
    def modelDelegateService
    def publicationService
    def redisService
    def springSecurityService
    def submissionService

    private String EXCH_DIR
    // The submission wizard validates the submission data in one request and completes the submission in the next one, so it
    // keeps the data here. The controller is a singleton, so this is shared by every request: the API's create and update
    // do not use it, they complete the submission they have built (JBM-798).
    Map<String, Object> validSubmissionDataMap = new HashMap<>()

    void afterPropertiesSet() throws Exception {
        EXCH_DIR = grailsApplication.config.jummp.vcs.exchangeDirectory
    }

    def completeSubmission() {
        Map result = doCompleteSubmission(validSubmissionDataMap)
        render(result as JSON)
    }

    /**
     * Says why a submission that has been validated cannot be completed, or null when it can be.
     *
     * A publication that is not valid does not stop it: the submission wizard does not stop for it either, and the API
     * only gets the publication's accession, so its submitter cannot fix the details.
     */
    private String reasonToRefuse(Map validation) {
        if (validation.areModelFilesValid && validation.areMetadataValid) {
            if (!validation.isPublicationValid) {
                logger.warn("Submitting although the publication is not valid: ${validation.errMsg}")
            }
            return null
        }
        // Completing it would fail as a server error and mail the admin for a file that was never uploaded (JBM-798)
        (validation.errMsg as String).trim()
    }

    private Map doCompleteSubmission(Map working) {
        String message = ""
        String status = "Success"
        try {
            /* The following statements aim at saving the new submission or updates */
            HashSet<String> result = submissionService.handleSubmission(working)

            /* Below is used for post processing submission and rendering the result to the callee */
            String modelId = working.get("modelId")
            working.put("accessType", "update")
            working.put("changesMade", result)
            boolean isUpdate = working.get("isUpdate") as boolean
            if (!isUpdate) {
                modelId = result.first()
                working.put("accessType", "create")
                working.put("changesMade", [])
            } else {
                HashSet<String> changesMade = working.get("changesMade") as HashSet<String>
                changesMade.addAll(result)
                HashSet<String> changes = changesMade.sort()
                working.put("changesMade", changes)
            }
            String modelURL = createLink(controller: "model", action: "show", params: [id: modelId], absolute: true)
            working.putAll(["modelId": modelId, "modelURL": modelURL])
            working.put("site", deployTarget)

            // Method 1: synchronous approach
            submissionService.processPostSubmission(working)

            // Method 2: asynchronous approach
            // TODO: investigate why the following async block fails due to
            /* org.springframework.jdbc.BadSqlGrammarException: Hibernate operation: could not extract ResultSet; bad SQL
        grammar [n/a]; nested exception is com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException: You have an
        error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right
        syntax to use near ')) and (aclentry1_.mask in (1 , 16)) and aclentry1_.granting=1 group by revision'  at
        line 17
         */
            /*Promises.task {
            String auditId = submissionService.processPostSubmission(working)
            Thread.sleep(5000)
            return auditId
        }.onComplete { auditId ->
            logger.info("The submission/update flow has been recorded with the id ${auditId}")
        }.onError { Throwable throwable ->
            logger.error("Errors while processing post submission {}", throwable)
        }*/

            /* Build the right messages to show at the model owner/submitter */
            Map msg = buildResultMessage(isUpdate, status, message, modelURL, modelId, working)
            message = msg.get("message")
            status = msg.get("status")
            return ["message": message, "status": status, "modelURL": modelURL, "modelIdentifier": modelId]
        } catch (Exception e) {
            status = "Failure"
            handleException(working, e)
            String errorTicketId = working.get("submissionFolder")
            message = groovyPageRenderer.render(template: "/templates/errorTemplate",
                plugin: "jummp-plugin-web-application", model: ["errorTicketId": errorTicketId])
            String cause = working.get("cause")
            return ["ticketID": errorTicketId, "status": status, "message": message, "cause": cause]
        }
    }

    /**
     * Create {@see RepositoryFileTransportCommand} object being used in the submission process
     *
     * @param submissionFolder
     * @param filename      A String denoting the file name
     * @param isModelFile   A boolean value denoting the file is the model file or not
     * @param description   A String denoting the file description
     *
     * @return {@see RepositoryFileTransportCommand} object
     */
    RFTC createRFTC(final String submissionFolder, final String filename,
                    final boolean isModelFile, final String description) {
        File modelDirectory = new File(EXCH_DIR, submissionFolder)
        File modelFile = new File(modelDirectory, filename)

        new RFTC(path: modelFile.getCanonicalPath(),
            mainFile: isModelFile, userSubmitted: true, hidden: false, description: description)
    }

    def processUploadFiles() {
        String submissionFolder = params.get("submissionFolder")
        String uploadingFiles = params.uploadingFiles.decodeHTML()
        def filesMap = JSON.parse(uploadingFiles)
        Map uploadedFiles = new HashMap()
        for (JSONElement e : filesMap) {
            e["submissionFolder"] = submissionFolder
            List fileErrors = validateFile(e)
            e["validateFileErrors"] = fileErrors
            // each file says what is wrong with it in the same way: empty, no description, invalid name. The upload
            // step puts the name of the file in front of every message.
            def description = e["description"]
            boolean hasDescription = description instanceof CharSequence && description.toString().trim()
            e["validateFileDescription"] = hasDescription ? [] : [FILE_DESCRIPTION_MISSING]
            uploadedFiles.put(e["filename"], e["originalFilesize"])
            if (e["isModelFile"] && fileErrors) {
                // A file that is missing, empty or a directory has nothing to detect, and reading an empty xml file
                // fails in the detection of its format (JBM-798). Send what the upload step reads, and its errors.
                e["detectedModelFormat"] = [:]
                e["validSyntax"] = false
                e["validateSyntaxErrors"] = []
                e["detectedModelInfo"] = [:]
            } else if (e["isModelFile"]) {
                // Presumably the submission has a single (main) model file
                Map detectedModelFormat = detectModelFormat(e)
                e["detectedModelFormat"] = detectedModelFormat
                List errors = []
                e["validSyntax"] = validateSyntax(e, detectedModelFormat.identifier as String, errors)
                e["validateSyntaxErrors"] = errors
                Map detectedModelInfo = detectModelInfo(e, detectedModelFormat.identifier as String)
                e["detectedModelInfo"] = detectedModelInfo
            }
            // check for the valid file name
            if (!FileHelper.isFileNameAcceptable(e["filename"] as String)) {
                e["validateFileName"] = [FILE_NAME_INVALID] as List<String>
            }
            // one line for the file, which the upload step shows with the name of the file in front of it
            e["validateFileSummary"] = sentences(fileErrors + e["validateFileDescription"] + (e["validateFileName"] ?: []))
        }
        // Determines which files are added and removed
        HashSet<String> changesMade = new ArrayList<String>()
        if (params.boolean("isUpdate")) {
            changesMade = inferChangesMadeOnModelFiles(uploadedFiles)
        }
        render([filesMap: filesMap, changesMade: changesMade] as JSON)
    }

    /** Joins messages into sentences: "The file is empty. The file needs a description." */
    static String sentences(List<String> messages) {
        messages.collect { String message ->
            String trimmed = message.trim()
            trimmed ==~ /.*[.!?]$/ ? trimmed : trimmed + "."
        }.join(" ")
    }

    def doLastValidateSubmissionData() {
        // TODO: check the data and save all the data to Redis or return false due to failure or incorrectness
        /**
         * This method is called twice:
         * (1) in submission.js when the step is 3 to show tick or cross icon
         * (2) clicking on the Submit button although the submission data have just been validated.
         */
        Map working = rebuildSubmissionData()
        Map result = doValidateSubmissionData(working)
        validSubmissionDataMap = working

        render(result as JSON)
    }

    private Map doValidateSubmissionData(Map working) {
        String errMsg
        // what each check found: this method is not the only one running, and it must not see what another request found
        List<String> messages = ["", "", ""]

        // 1. Check the uploaded files
        boolean areModelFilesValid = doValidateUploadedFiles(working, messages)
        // 2. Check the model metadata provided/updated
        boolean areMetadataValid = doValidateModelInfo(working, messages)
        // 3. Check the publication details
        boolean isPublicationValid = doValidatePublication(working, messages)
        errMsg = messages.findAll { it }.join("\n")
        Map<String, Object> result = new HashMap<>()
        String submitterInfo = working.get("submitterInfo")
        result.put("submitterInfo", submitterInfo)
        String submissionFolder = working.get("submissionFolder")
        result.put("submissionFolder", submissionFolder)
        result.put("errMsg", errMsg)
        result.put("areModelFilesValid", areModelFilesValid)
        result.put("areMetadataValid", areMetadataValid)
        result.put("isPublicationValid", isPublicationValid)
        boolean currentValidation = areModelFilesValid && areMetadataValid && isPublicationValid
        result.put("currentValidation", currentValidation)
        String strResult = toString(result)
        logger.debug("The result of verifying the submission data: \n$strResult")
        println("The result of verifying the submission data: \n$strResult")
        result
    }

    private boolean doValidateUploadedFiles(Map working, List<String> messages) {
        List<RFTC> rftcList = working.get("repository_files")
        String errFileMsg = ""
        boolean valid = true
        for (RFTC rftc : rftcList) {
            File file = new File(rftc.path)
            // a file that is missing or empty makes the files invalid (JBM-798; only the missing one did before), and
            // so does a directory, which has a length too
            if (file.isDirectory()) {
                errFileMsg += "${file.name}: The model file cannot be a directory.\n"
                valid = false
            } else if (!file.exists() || file.length() <= 0) {
                errFileMsg += "${file.name}: Not found or not exist or empty.\n"
                valid = false
            }
        }
        messages[0] = errFileMsg
        valid
    }

    private boolean doValidateModelInfo(Map working, List<String> messages) {
        RTC revision = working.get("RevisionTC") as RTC
        String errMsg = ""
        // 1. Condition 1: model format is not null
        boolean mfCond = revision.format
        if (!mfCond) {
            errMsg += "Model format is missing.\n"
        }
        // 2. Condition 2: model approach is not null
        boolean maCond = working.containsKey("modelling_approach")
        if (!maCond) {
            errMsg += "Modelling approach is missing.\n"
        }
        // 3. Condition 3: model name is not null
        boolean mnCond = revision.model.name
        if (!mnCond) {
            errMsg += "Model name is empty or blank.\n"
        }
        messages[1] = errMsg
        mfCond && maCond && mnCond
    }

    private boolean doValidatePublication(Map working, List<String> messages) {
        MTC model = working.get("ModelTC") as MTC
        String errMsg = ""
        if (!model.publication) {
            return true
        } else {
            boolean r = model.publication.validate()
            if (!r) {
                errMsg += "Publication record is invalid.\n"
            }
            messages[2] = errMsg
            return r
        }
    }

    def validateModelInfo() {
        HashSet<String> changesMade = inferChangesMadeOnModelInfo()
        render([status: "OK", changesMade: changesMade] as JSON)
    }

    def checkCurrentValidation() {
        HashSet<String> changesMade = params.list("changesMade[]").toSet()
        if (!changesMade) { changesMade = new HashSet<>() } else {
            CollectionHelper.remove(changesMade, "MODEL PUBLICATION")
        }
        changesMade.add("MODEL PUBLICATION: Removed the publication details.")
        render([status: "OK", changesMade: changesMade] as JSON)
    }

    def renderFileUploadFailures() {
        render([status: "OK"] as JSON)
    }

    /**
     * This method is designed to serve the create process called via REST API
     * @return rendering the result map to an JSON object
     */
    def create() {
        String metadata = request.reader.text
        logger.info("Creating the new submission: $metadata")
        Map<String, Object> working = [
            isUpdate: false, isUpdateOnExistingModel: false,
            isAmend: false, isMetadataSubmission: false,
            accessType: "create", accessFormat: "json"] as HashMap<String, Object>
        if (metadata) {
            makeSubmission(metadata, working)
        } else {
            String msg = "Cannot create the model as requested because of the empty input."
            logger.debug(msg)
            render([message: msg, status: 400] as JSON)
        }
    }

    /**
     * This method is designed to serve the update process called via REST API
     * @return rendering the result map to an JSON object
     */
    def update() {
        String metadata = request.reader.text
        logger.info("Updating the submission: $metadata")
        Map<String, Object> working = [
            isUpdate: true, isUpdateOnExistingModel: true,
            isAmend : false, isMetadataSubmission: false,
            accessType: "update", accessFormat: "json"] as Map<String, Object>
        if (metadata) {
            makeSubmission(metadata, working)
        } else {
            String msg = "Cannot update the model as requested because of the empty input."
            logger.debug(msg)
            render([message: msg, status: 400] as JSON)
        }
    }

    private void makeSubmission(String metadata, Map working) {
        String uuid = request.getHeader("SubmissionFolder")
        working.put("submissionFolder", uuid)
        def currentUser = springSecurityService.currentUser
        working.put("submitterInfo", [userRealName: currentUser?.person?.userRealName,
                                      username: currentUser.username, email: currentUser.email])
        Map map
        try {
            submissionService.buildFromJSONFile(metadata, working)
        } catch (Exception e) {
            // when the service knows why it refuses the submission it says so in the working memory, and it has not
            // put the repository files there yet (JBM-796)
            String reason = (working["cause"] ?: e.message ?: "The submission cannot be built from the metadata.") as String
            logger.error("Refusing the submission: $reason")
            map = [message: reason, status: 400]
        }
        if (map == null) {
            String reason = reasonToRefuse(doValidateSubmissionData(working))
            if (reason == null) {
                // the submission that was built, not validSubmissionDataMap, which other requests share
                map = doCompleteSubmission(working)
            } else {
                // as in the submission wizard, where the submitter cannot go on either
                logger.error("Refusing the submission: $reason")
                map = [message: reason, status: 400]
            }
        }

        withFormat {
            json { render map as JSON }
            xml { render map as XML }
            '*' { render status: 415, view: "/errors/error415" }
        }
    }

    private List validateFile(final JSONElement file) {
        logger.debug("Validating the file: $file")
        File modelFile = fileSystemService.retrieve(file)
        List validationErrors = submissionService.validateFile(modelFile)
        return validationErrors
    }

    private boolean validateSyntax(final JSONElement file, final String format, final List<String> errors) {
        logger.debug("Validating the file: $file")
        File modelFile = fileSystemService.retrieve(file)
        boolean valid = submissionService.validateSyntax(modelFile, format, errors)
        return valid
    }

    private Map detectModelFormat(final JSONElement jsonFileData) {
        logger.debug("Detecting model format for the file $jsonFileData")
        String submissionFolder = jsonFileData["submissionFolder"]
        String filename = jsonFileData["filename"]
        String description = jsonFileData["description"]
        RFTC mfRFTC = createRFTC(submissionFolder, filename, true, description)
        MFTC format = modelFileFormatService.inferModelFormat([mfRFTC])

        // by the way, detecting publication annotations included in the main file, however, we select the first one
        Map pubDetails = [:]
        RTC revTC = new RTC(files: [mfRFTC], format: format)
        List<String> pubURIs = modelFileFormatService.getPublicationAnnotations(revTC)
        if (pubURIs) {
            logger.info("""Detected publication identifiers included in the file $filename as annotations: \
${pubURIs?.join(";")}""")
            Map pubMeta = resolvePublicationMetadata(pubURIs)
            if (pubMeta) { pubDetails.putAll(pubMeta) }
        }

        // TODO: add "readme": "not decided yet" with an updated value to the returned map
        pubDetails.putAll(["identifier": format.identifier, "name": format.name, "id": format.id])
        return pubDetails
    }

    private static Map resolvePublicationMetadata(List<String> pubURIs) {
        String firstPubURI = pubURIs?.first()
        Map result = [:]
        try {
            String rest = JummpHttpService.getDataTypeAndAccession(firstPubURI)
            if (!rest) {
                logger.error("""Cannot fetch the publication details from ${firstPubURI} due to not resolving \
data type and accession from the URI.""")
                return null
            }
            String json = JummpHttpService.jsonGetRequest("https://resolver.api.identifiers.org/" + rest)
            JSONObject jsonObject = new JSONObject(json)
            JSONObject parsedCI = jsonObject.getJSONObject("payload").getJSONObject("parsedCompactIdentifier")
            String localId = parsedCI.getString("localId")
            String namespace = parsedCI.getString("namespace")
            String collectionLabel
            if ("pubmed" == namespace) {
                collectionLabel = "PubMed ID"
            } else if ("doi" == namespace) {
                collectionLabel = "DOI"
            } else {
                collectionLabel = "unknown"
            }
            result = ["pubURI": firstPubURI, "namespace": namespace,
                      "collectionLabel": collectionLabel, "accession": localId]
        } catch (NullPointerException npe) {
            logger.error("Cannot resolve the publication metadata for ${firstPubURI} because of NPE (${npe.message})!")
        } finally {

        }
        return result
    }

    /**
     * This service detects three info of the model such as name, description and modelling approach based on {@link
     * ModelFileFormatService} which basically reads the main model file and extracts these info.
     *
     * @param fileJSONData  A JSON string representing the input data as the uploading files
     * @param modelFormat   A String denoting the model format name
     * @return              A Map of three items and their associated values
     */
    private Map detectModelInfo(final JSONElement fileJSONData, final String modelFormat) {
        logger.debug("Detecting and extracting the model info from: $fileJSONData")
        File modelFile = fileSystemService.retrieve(fileJSONData)
        Map modelInfo = submissionService.detectModelInfo(modelFile, modelFormat)
        // TODO: load other info from cache and update this object modelInfo.put("otherInfo", "experimental data")
        return modelInfo
    }

    private HashSet<String> inferChangesMadeOnModelFiles(Map uploadedFiles) {
        HashSet<String> changesMade = params.list("changesMade[]").toSet()
        if (!changesMade) { changesMade = new HashSet<>() } else {
            CollectionHelper.remove(changesMade, "MODEL FILES")
        }
        List parsedExistingFiles = JSON.parse(params.files.decodeHTML()) as List
        for (JSONElement e : (parsedExistingFiles as List<JSONElement>)) {
            boolean exists = uploadedFiles.find { String fName, String fSize ->
                long size = Long.parseLong(fSize)
                e["filename"] == fName && e["size"] == size
            }
            if (!exists) {
                changesMade.add("MODEL FILES: Removed file ${e.filename}".toString())
            }
        }
        uploadedFiles.each { String fName, String fSize ->
            long size = Long.parseLong(fSize)
            boolean exists = parsedExistingFiles.find {
                it["filename"] == fName && it["size"] == size
            }
            if (!exists) {
                changesMade.add("MODEL FILES: Added file ${fName}".toString())
            }
        }
        changesMade
    }

    private HashSet<String> inferChangesMadeOnModelInfo() {
        HashSet<String> changesMade = params.list("changesMade[]")
        if (!changesMade) { changesMade = new HashSet<>() } else {
            CollectionHelper.remove(changesMade, "MODEL INFO")
        }
        if (params.boolean("isUpdate")) {
            final String latestName = params.latestModelName.decodeHTML()
            final String submissionFolder = params.submissionFolder.decodeHTML()
            final String latestDescription = redisService.doRedisHGet(submissionFolder, "latestModelDescription")
            final String editedName = params.editedModelName.decodeHTML()
            final String editedDescription = params.editedModelDescription.decodeHTML()
            if (latestName != editedName) {
                changesMade.add("MODEL INFO: Edited the model name.")
            }
            if (latestDescription != editedDescription) {
                changesMade.add("MODEL INFO: Edited the short submission description.")
            }

            final String latestModelFormat = params.latestModelFormat.decodeHTML()
            final String latestModelFormatNameAndVersion = params.latestModelFormatNameAndVersion.decodeHTML()
            final String origFormat = "$latestModelFormat (${latestModelFormatNameAndVersion})"

            final String editedModelFormat = params.editedModelFormat.decodeHTML()
            final String editedModelFormatNameAndVersion = params.editedModelFormatNameAndVersion.decodeHTML()
            final String newFormat = "$editedModelFormat (${editedModelFormatNameAndVersion})"
            if (latestModelFormat != editedModelFormat) {
                changesMade.add("MODEL INFO: Changed the model format from $origFormat to $newFormat.")
            } else {
                final String latestReadmeSubmission = params.latestReadmeSubmission.decodeHTML()
                final String editedReadmeSubmission = params.editedReadmeSubmission.decodeHTML()
                if (latestReadmeSubmission != editedReadmeSubmission) {
                    changesMade.add("MODEL INFO: Edited the submission readme.")
                }
            }

            final String latestModellingApproach = params.latestModellingApproach.decodeHTML()
            final String editedModellingApproach = params.editedModellingApproach.decodeHTML()
            if (!latestModellingApproach) {
                changesMade.add("MODEL INFO: Added the modelling approach.")
            } else if (latestModellingApproach != editedModellingApproach) {
                String msg = "MODEL INFO: Changed the modelling approach from $latestModellingApproach to $editedModellingApproach.".toString()
                changesMade.add(msg)
            } else {
                final String latestOtherInfo = params.latestOtherInfo.decodeHTML()
                final String editedOtherInfo = params.editedOtherInfo.decodeHTML()
                if (latestOtherInfo != editedOtherInfo) {
                    changesMade.add("MODEL INFO: Edited the other info.")
                }
            }
        }
        changesMade
    }

    private void handleException(final Map working, final Exception e) {
        logger.error("Oops!!! There has been an error!", e)
        // rollback and backup submission
        String ticket = working.get("submissionFolder")
        final File PARENT = new File(EXCH_DIR)
        File submissionFiles = new File(PARENT, ticket)
        File buggyFiles = new File(PARENT, "buggy")
        File temporaryStorage = new File(buggyFiles, ticket)
        temporaryStorage.mkdirs()
        if (working.containsKey("repository_files")) {
            List repFiles = working.get("repository_files") as List
            if (repFiles && submissionFiles.exists()) {
                FileUtils.copyDirectory(submissionFiles, temporaryStorage)
            } else {
                logger.error("The submission files are not available for now!")
            }
        }

        // create error.log containing the output of ExceptionUtils.getStackTrace(e)
        File errorLog = new File(temporaryStorage, "error.log")
        errorLog.write(ExceptionUtils.getStackTrace(e))
        logger.error(ExceptionUtils.getRootCauseMessage(e))
        // save the submission metadata to submission.log
        File submissionLog = new File(temporaryStorage, "submission.log")
        def msg = working.containsKey("modelId") ?
                    "Submission Data of ${working.get('modelId')}\n" : "Submission Data\n"
        submissionLog.write(msg)
        working.each {
            submissionLog.append("${it.key}: ${it.dump()}\n")
        }
        List<RFTC> filesList = working.get("repository_files") as List<RFTC>
        submissionLog.append("\nDump of the repository files:\n")
        for (RFTC fileTC : filesList) {
            submissionLog.append(fileTC.dump())
        }
        submissionLog.append("\nDump of the revision transport command:\n")
        RTC revisionTC = working.get("RevisionTC") as RTC
        submissionLog.append(revisionTC.dump())
        println(submissionLog.text) // sending the logs to the stdout is used for K8s ELK

        submissionService.cleanup(working)
        mailingService.send([to: grailsApplication.config.jummp.security.registration.email.adminAddress as String,
                             subject: "Bug in submission: ${ticket}",
                             text: "MESSAGE: ${ExceptionUtils.getStackTrace(e)}"])
    }

    private Map rebuildSubmissionData() {
        /* The following statements aim at saving the new submission or updates */
        Map working = new HashMap<String, Object>()
        working.put("submitterInfo", params.get("submitterInfo").decodeHTML())
        String submissionFolder = params.get("submissionFolder").decodeHTML() as String
        working.put("submissionFolder", submissionFolder)

        // 1. Rebuild the uploaded files
        List<RFTC> rftcList = new ArrayList<RFTC>()
        rftcList = rebuildRepoFiles(params.modelFile.decodeHTML() as String,
            params.additionalFiles.decodeHTML() as String, working)
        // 2. Rebuild the model format
        MFTC format = modelFileFormatService.inferModelFormat(rftcList)
        working.put("model_format", format)

        boolean isUpdate = params.boolean("isUpdate")
        boolean isAmend = params.boolean("isAmend")
        // Only an update can be flagged minor - a brand new model has no earlier revision to
        // be "minor" relative to. Submitters (for their own models), curators and admins can
        // all reach this update flow, so the checkbox is available to all three.
        boolean isMinorRevision = isUpdate && params.boolean("isMinorRevision")
        boolean isMetadataSubmission = params.boolean("isMetadataSubmission")
        working.put("isUpdate", isUpdate)
        working.put("isAmend", isAmend)
        working.put("isMinorRevision", isMinorRevision)
        working.put("isMetadataSubmission", isMetadataSubmission)
        MTC model = new MTC()
        if (isUpdate) {
            model = modelDelegateService.getModel(params.modelId)
            working.put("modelId", params.modelId)
        }
        RTC revision = new RTC(model: model, format: format, minorRevision: false, validated: true)
        String modelId = params.modelId
        working.put("modelId", modelId)
        if (isUpdate) {
            revision = modelDelegateService.getLatestRevision(modelId, false)
            revision.minorRevision = isMinorRevision
            final String latestDescription = redisService.doRedisHGet(submissionFolder, "latestModelDescription")
            working.putAll(["latestModelName": params.latestModelName.decodeHTML(),
                            "latestModelDescription": latestDescription])
            // Snapshot the pre-update baseline file list under its own key *before*
            // populateDataRevision() below overwrites revision.files with this submission's
            // own new file list. SubmissionService.NewRevisionStateMachine.completeSubmission()
            // needs this baseline (not the new list) to detect additional files the submitter
            // removed, so they get deleted from VCS instead of silently lingering there
            // untracked (JBM-764) - accessing revision.files here, while it's still unset,
            // lazily fetches the latest revision's actual current files.
            working.put("previousRevisionFiles", revision.files)
        }

        // rebuild the model info as much as possible detected from the former step
        // and update them in the working map
        rebuildModelInfo(params.modelInfo?.decodeHTML() as String, rftcList, working, model)

        // populate publication details
        if (params.publication?.decodeHTML() != "\"\"" && params.publication.decodeHTML() != "{}") {
            populatePublication(params.publication?.decodeHTML(), model)
        }

        // populate the data on the revision
        String revisionComments = params.revisionComments?.decodeHTML() as String
        populateDataRevision(revision, model, working, rftcList, revisionComments, isUpdate)

        working.put("ModelTC", model)
        working.put("RevisionTC", revision)
        working.put("isUpdateOnExistingModel", isUpdate)
        working.put("shouldCreateNewRevision", true) // TODO: allow curators decide
        // Please review the callee where the changesMade Set is converted to changesMade List.
        // The callee is an ajax invoking the completeSubmission action where it is invoking to
        // this method. Therefore, this method can see the params object.
        working.put("changesMade", params.list("changesMade[]"))

        String lcr = params.latestContributorRole?.decodeHTML() as String
        String contributorRole = lcr.indexOf(":") > 0 ? lcr.take(lcr.indexOf(":")) : lcr
        working.put("contributorRole", contributorRole)

        return working
    }

    private List<RFTC> rebuildRepoFiles(String paramModelFile, String paramAdditionalFiles,
                                        HashMap<String, Object> working) {
        List<RFTC> rftcList = new ArrayList<>()
        JSONElement mf = JSON.parse(paramModelFile)
        RFTC mfRFTC = createRFTC(mf["submissionFolder"] as String, mf["filename"] as String, true, mf["description"] as String)
        rftcList.add(mfRFTC)
        def afs = JSON.parse(paramAdditionalFiles)
        for (def file : afs) {
            mfRFTC = createRFTC(file["submissionFolder"] as String,
                file["filename"] as String, false, file["description"] as String)
            rftcList.add(mfRFTC)
        }
        working.put("repository_files", rftcList)
        return rftcList
    }

    private void rebuildModelInfo(String modelInfo, List<RFTC> files,
                                  HashMap<String, Object> working, MTC model) {
        def modelInfoData = JSON.parse(modelInfo)
        RFTC modelFile = files.find { it.mainFile }
        model.name = modelInfoData["detectedName"] ?: modelFile.filename
        model.description = modelInfoData["detectedDescription"] ?: ""
        def detectedModelling = modelInfoData["detectedModelling"] ?
            modelInfoData["detectedModelling"]["approach"] : ""
        working.put("modelling_approach", detectedModelling)
        def otherInfo = modelInfoData["detectedModelling"] ? modelInfoData["detectedModelling"]["otherInfo"] : ""
        working.put("other_info", otherInfo)
        def detectedModelFormat = modelInfoData["detectedModelFormat"] ?
            modelInfoData["detectedModelFormat"]["id"] : null
        working.put("model_format", detectedModelFormat)
        def readme = modelInfoData["detectedModelFormat"] ? modelInfoData["detectedModelFormat"]["readme"] : ""
        working.put("readme_submission", readme)
    }

    private void populatePublication(def paramPublication, MTC model) {
        if (paramPublication != "{}" && paramPublication) {
            Map publicationData = publicationService.buildPublicationFromJSONData(paramPublication)
            model.publication = publicationData["publication"]
        } else {
            model.publication = null
        }
    }

    private void populateDataRevision(RTC revision, MTC model, Map working,
                                      ArrayList<RFTC> rftcList, String paramComments,
                                      boolean isUpdate = false) {
        revision.files = rftcList
        revision.model = model
        revision.name = model.name
        revision.description = model.description
        if (!isUpdate) {
            // preserve the following properties when updating the model
            revision.curationState = CurationState.NON_CURATED
            revision.validationLevel = ValidationState.APPROVE
        }
        // On an update/amend, `revision` here is the latest revision's own RTC (see
        // rebuildSubmissionData()) - its .comment already holds *that* revision's old commit
        // message, carried over as a base for the fields we do want to inherit (name,
        // description, format, ...). The default below must therefore be driven by whether the
        // submitter actually typed something this time (paramComments), never by whether
        // revision.comment happens to be non-blank - it always is on an update, which used to
        // make this branch a no-op and silently leave the previous revision's message in place
        // whenever the comment box was left empty.
        if (paramComments) {
            revision.comment = paramComments
        } else {
            revision.comment = "Model revised without commit message"
        }
        working.put("new_name", revision.name)
        working.put("new_description", revision.description)
        working.put("RevisionTC", revision)
    }

    private Map buildResultMessage(boolean isUpdate, String status, String message,
        String modelURL, String modelId, Map<String, Object> working) {
        if (isUpdate) {
            if (working.get("changesMade")) {
                status = "Success"
                message = groovyPageRenderer.render(template: "/templates/model/submit/subviews/successUpdate",
                    plugin: "jummp-plugin-web-application", model: ["modelURL": modelURL, "modelId": modelId])
            } else {
                status = "Failure"
                message = groovyPageRenderer.render(template: "/templates/model/submit/subviews/failureUpdate",
                    plugin: "jummp-plugin-web-application")
            }
        } else {
            if (modelId) {
                status = "Success"
                message = groovyPageRenderer.render(template: "/templates/model/submit/subviews/successSubmission",
                    plugin: "jummp-plugin-web-application", model: ["modelURL": modelURL, "modelId": modelId])
            } else {
                status = "Failure"
                message = groovyPageRenderer.render(template: "/templates/model/submit/subviews/failureSubmission",
                    plugin: "jummp-plugin-web-application")
            }
        }
        [status: status, message: message] as Map<String, String>
    }

    private String toString(Map<String, Object> working) {
        String result = "[\n"
        result += "\tSubmitter Info: ${working.get("submitterInfo")}\n"
        result += "\tSubmission Folder: ${working.get("submissionFolder")}\n"
        result += "\tError Message: ${working.get("errMsg")}\n"
        result += "\tareModelFilesValid: ${working.get("areModelFilesValid")}\n"
        result += "\tareMetadataValid: ${working.get("areMetadataValid")}\n"
        result += "\tisPublicationValid: ${working.get("isPublicationValid")}\n"
        result += "\tcurrentValidation: ${working.get("currentValidation")}\n"
        result += "]"
        result
    }
}
