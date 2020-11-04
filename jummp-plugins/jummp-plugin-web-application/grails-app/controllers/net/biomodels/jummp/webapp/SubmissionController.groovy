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
import net.biomodels.jummp.core.InvalidPublicationAuthorsException
import net.biomodels.jummp.core.model.CurationState
import net.biomodels.jummp.core.model.ModelFormatTransportCommand as MFTC
import net.biomodels.jummp.core.model.ModelTransportCommand as MTC
import net.biomodels.jummp.core.model.PublicationTransportCommand
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand as RFTC
import net.biomodels.jummp.core.model.RevisionTransportCommand as RTC
import net.biomodels.jummp.core.model.ValidationState
import org.codehaus.groovy.grails.web.json.JSONElement
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Secured(['IS_AUTHENTICATED_FULLY'])
class SubmissionController {
    private static final Logger logger = LoggerFactory.getLogger(SubmissionController.class)
    def fileSystemService
    def groovyPageRenderer
    def messageSource
    def modelFileFormatService
    def modelDelegateService
    def publicationService
    def submissionService

    def completeSubmission() {
        /* The following statements aims at saving the new submission or updates */
        Map working = new HashMap<String, Object>()
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
        MTC model = new MTC()
        if (isUpdate && params.modelId) {
            model = modelDelegateService.getLatestRevision(params.modelId, false).model
        }

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
            Map publicationData = buildPublicationFromJSONData(params.publication.decodeHTML())
            model.publication = publicationData["publication"]
        } else {
            model.publication = null
        }
        working.put("isUpdateOnExistingModel", isUpdate)
        working.put("shouldCreateNewRevision", true) // TODO: allow curators decide

        RTC revision = new RTC(files: rftcList, model: model, format: format)

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
        // test this first
        submissionService.processPostSubmission(working)
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
        String message = ""
        String status = "Success"
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
    }

    Map buildPublicationFromJSONData(final String JSONData) {
        // TODO: unite this method to the same in PublicationController
        //PDEC pubContext = publicationMap.get(flow.workingMemory.get("SelectedPubLinkProvider"))
        PublicationTransportCommand tempPTC = new PublicationTransportCommand()//pubContext.publication
        def pubDetails = JSON.parse(JSONData)
        bindData(tempPTC, pubDetails, [exclude: ['authors']])
//        tempPTC.linkProvider = publicationService.inferPublicationLinkProvider(pubDetails.linkProvider)
        String message = ""
        String status = ""
        List errors = new ArrayList()
        try  {
            publicationService.assembleAuthors(tempPTC, pubDetails.authors)
            message = "Authors have been successfully assembled"
            status = "Success"
        } catch (InvalidPublicationAuthorsException e) {
            String errMsg = e.getI18nErrorMessage4InvalidAuthor()
            message = "There have been errors while parsing authors of the publication:<br/>${errMsg}"
            status = "Error"
        }
        if (tempPTC.hasErrors()) {
            def locale = Locale.getDefault()
            for (fieldErrors in tempPTC.errors) {
                for (error in fieldErrors.allErrors) {
                    message = messageSource.getMessage(error, locale)
                    errors.add(message)
                    logger.error(message)
                }
            }
            status = "Error"
        }
        ["message": message, "status": status, "errors": errors, "publication": tempPTC] as Map
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
        File subFolder = new File(exchangeDir, submissionFolder)
        File modelFile = new File(subFolder, filename)

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
                List messages = validateSyntax(e, detectedModelFormat.identifier)
                e["validateSyntaxErrors"] = messages
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

    private List<String> validateSyntax(final JSONElement file, final String format) {
        logger.debug("Validating the file: $file")
        File modelFile = fileSystemService.retrieve(file)
        List validationErrors = submissionService.validateSyntax(modelFile, format)
        return validationErrors
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
        List parsedExistingFiles = JSON.parse(params.files) as List
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
}
