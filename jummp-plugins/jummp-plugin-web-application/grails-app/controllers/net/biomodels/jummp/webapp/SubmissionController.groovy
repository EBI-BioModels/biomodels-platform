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
import grails.plugin.springsecurity.annotation.Secured
import net.biomodels.jummp.core.model.CurationState
import net.biomodels.jummp.core.model.ModelFormatTransportCommand as MFTC
import net.biomodels.jummp.core.model.ModelTransportCommand as MTC
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand as RFTC
import net.biomodels.jummp.core.model.RevisionTransportCommand as RTC
import net.biomodels.jummp.core.model.ValidationState
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.codehaus.groovy.grails.web.json.JSONElement
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Secured(['IS_AUTHENTICATED_FULLY'])
class SubmissionController {
    private static final Logger logger = LoggerFactory.getLogger(SubmissionController.class)
    def fileSystemService
    def grailsApplication
    def groovyPageRenderer
    def mailService
    def messageSource
    def modelFileFormatService
    def modelDelegateService
    def publicationService
    def submissionService

    def completeSubmission() {
        String message = ""
        String status = "Success"
        Map working = new HashMap<String, Object>()
        working.put("submissionFolder", params.get("submissionFolder"))
        try {
            /* The following statements aims at saving the new submission or updates */
            List<RFTC> rftcList = new ArrayList<RFTC>()
            JSONElement mf = JSON.parse(params.modelFile.decodeHTML())
            RFTC mfRFTC = createRFTC(mf["submissionFolder"], mf["filename"], true, mf["description"])
            rftcList.add(mfRFTC)
            def afs = JSON.parse(params.additionalFiles.decodeHTML())
            for (def file : afs) {
                mfRFTC = createRFTC(file["submissionFolder"], file["filename"], false, file["description"])
                rftcList.add(mfRFTC)
            }
            working.put("repository_files", rftcList)
            MFTC format = modelFileFormatService.inferModelFormat(rftcList)

            boolean isUpdate = params.boolean("isUpdate")
            boolean isAmend = params.boolean("isAmend")
            working.put("isAmend", isAmend)
            MTC model = new MTC()
            if (isUpdate) {
                model = modelDelegateService.getModel(params.modelId)
            }
            RTC revision = new RTC(model: model, format: format)
            if (isAmend && params.modelId) {
                revision = modelDelegateService.getLatestRevision(params.modelId, false)
            }
            revision.files = rftcList

            // populate model info
            def modelInfoData = JSON.parse(params.modelInfo.decodeHTML())
            model.name = modelInfoData["detectedName"] ?: mf["filename"]
            model.description = modelInfoData["detectedDescription"] ?: ""
            working.put("modelling_approach", modelInfoData["detectedModelling"]["approach"])
            working.put("other_info", modelInfoData["detectedModelling"]["otherInfo"])
            working.put("model_format", modelInfoData["detectedModelFormat"]["id"])
            working.put("readme_submission", modelInfoData["detectedModelFormat"]["readme"])

            // populate publication details
            if (params.publication) {
                Map publicationData = publicationService.buildPublicationFromJSONData(params.publication.decodeHTML())
                model.publication = publicationData["publication"]
            } else {
                model.publication = null
            }
            working.put("isUpdateOnExistingModel", isUpdate)
            working.put("shouldCreateNewRevision", true) // TODO: allow curators decide

            revision.model = model
            revision.name = model.name
            revision.description = model.description
            revision.validated = true
            revision.minorRevision = false
            revision.curationState = CurationState.NON_CURATED
            revision.validationLevel = ValidationState.APPROVE
            revision.comment = params.revisionComments.decodeHTML() ?: "Model revised without commit message"
            working.put("new_name", revision.name)
            working.put("new_description", revision.description)
            working.put("RevisionTC", revision)
            working.put("changesMade", params.list("changesMade[]"))
            HashSet<String> result = submissionService.handleSubmission(working)

            /* Below is used for post processing submission and rendering the result to the callee */
            String modelId = params.modelId
            working.put("accessType", "update")
            working.put("changesMade", result)
            if (!isUpdate) {
                modelId = result.first()
                working.put("accessType", "create")
                working.put("changesMade", [])
            }
            String modelURL = createLink(controller: "model", action: "show", params: [id: modelId])
            working.putAll(["modelId": modelId, "modelURL": modelURL])

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
            render(["message": message, "status": status, "modelURL": modelURL, "modelIdentifier": modelId] as JSON)
        } catch (Exception e) {
            status = "Failure"
            handleException(working, e)
            String errorTicketId = working.get("submissionFolder")
            message = groovyPageRenderer.render(template: "/templates/errorTemplate",
                plugin: "jummp-plugin-web-application", model: ["errorTicketId": errorTicketId])
            render(["ticketID": errorTicketId, "status": status, "message": message] as JSON)
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
        String exchangeDir = grailsApplication.config.jummp.vcs.exchangeDirectory
        File modelDirectory = new File(exchangeDir, submissionFolder)
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
            e["validateFileErrors"] = validateFile(e)
            uploadedFiles.put(e["filename"], e["originalFilesize"])
            if (e["isModelFile"]) {
                // Presumably the submission has a single (main) model file
                Map detectedModelFormat = detectModelFormat(e)
                e["detectedModelFormat"] = detectedModelFormat
                List errors = []
                e["validSyntax"] = validateSyntax(e, detectedModelFormat.identifier, errors)
                e["validateSyntaxErrors"] = errors
                Map detectedModelInfo = detectModelInfo(e, detectedModelFormat.identifier)
                e["detectedModelInfo"] = detectedModelInfo
            }
        }
        // Determines which files are added and removed
        List<String> changesMade = new ArrayList<String>()
        if (params.boolean("isUpdate")) {
            changesMade = inferChangesMade(uploadedFiles)
        }
        render([filesMap: filesMap, changesMade: changesMade] as JSON)
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
        return ["identifier": format.identifier, "name": format.name, "id": format.id]
    }

    private Map detectModelInfo(final JSONElement fileJSONData, final String modelFormat) {
        logger.debug("Detecting and extracting the model info from: $fileJSONData")
        File modelFile = fileSystemService.retrieve(fileJSONData)
        Map modelInfo = submissionService.detectModelInfo(modelFile, modelFormat)
        return modelInfo
    }

    private List<String> inferChangesMade(Map uploadedFiles) {
        List<String> changesMade = new ArrayList<>()
        List parsedExistingFiles = JSON.parse(params.files.decodeHTML()) as List
        for (JSONElement e : parsedExistingFiles) {
            boolean exists = uploadedFiles.find { String fName, String fSize ->
                long size = Long.parseLong(fSize)
                e["filename"] == fName && e["size"] == size
            }
            if (!exists) {
                changesMade.add("Removed file ${e.filename}")
            }
        }
        uploadedFiles.each { String fName, String fSize ->
            long size = Long.parseLong(fSize)
            boolean exists = parsedExistingFiles.find {
                it["filename"] == fName && it["size"] == size
            }
            if (!exists) {
                changesMade.add("Added file ${fName}")
            }
        }
        changesMade
    }

    private void handleException(final Map working, final Exception e) {
        logger.error("Oops!!! There has been an error!", e)
        // rollback and backup submission
        String ticket = working.get("submissionFolder")
        final String EXCHANGE = grailsApplication.config.jummp.vcs.exchangeDirectory
        final File PARENT = new File(EXCHANGE)
        File submissionFiles = new File(PARENT, ticket)
        File buggyFiles = new File(PARENT, "buggy")
        File temporaryStorage = new File(buggyFiles, ticket)
        temporaryStorage.mkdirs()
        if (working.containsKey("repository_files")) {
            List repFiles = working.get("repository_files")
            if (repFiles) {
                FileUtils.copyDirectory(submissionFiles, temporaryStorage)
            }
        }

        // create error.log containing the output of ExceptionUtils.getStackTrace(e)
        File errorLog = new File(temporaryStorage, "error.log")
        errorLog.write(ExceptionUtils.getStackTrace(e))
        // save the submission metadata to submission.log
        File submissionLog = new File(temporaryStorage, "submission.log")
        submissionLog.write("Submission Data\n")
        working.each {
            submissionLog.append("${it.key}: ${it.dump()}\n")
        }

        submissionService.cleanup(working)
        mailService.sendMail {
            to grailsApplication.config.jummp.security.registration.email.adminAddress
            from grailsApplication.config.jummp.security.registration.email.sender
            subject "Bug in submission: ${ticket}"
            body "MESSAGE: ${ExceptionUtils.getStackTrace(e)}"
        }
    }
}
