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

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import net.biomodels.jummp.core.InvalidPublicationAuthorsException
import net.biomodels.jummp.core.model.PublicationTransportCommand
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand as RFTC
import net.biomodels.jummp.core.model.ModelFormatTransportCommand as MFTC
import net.biomodels.jummp.core.model.ModelTransportCommand as MTC
import net.biomodels.jummp.core.model.RevisionTransportCommand as RTC
import org.codehaus.groovy.grails.web.json.JSONElement
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Secured(['IS_AUTHENTICATED_FULLY'])
class SubmissionController {
    private static final Logger logger = LoggerFactory.getLogger(PublicationController.class)
    def messageSource
    def modelFileFormatService
    def publicationService
    def submissionService
    def fileSystemService

    def completeSubmission() {
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
        MTC model = new MTC()


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
        working.put("shouldCreateNewRevision", true)

        RTC revision = new RTC(files: rftcList, model: model, format: format)

        revision.model = model
        revision.name = model.name
        revision.description = model.description
        revision.validated = true
        working.put("RevisionTC", revision)
        HashSet<String> result = submissionService.handleSubmission(working)

        String modelId = result.first()
        String modelURL = createLink(controller: "model", action: "show", params: [id: modelId])
        render(["message": "Everything is fine", "status": "Success", "modelURL": modelURL, "modelIdentifier": modelId]
            as JSON)
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
        for (JSONElement e : filesMap) {
            e["submissionFolder"] = submissionFolder
            e["validateFileErrors"] = validateFile(e)
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
        render filesMap as JSON
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
}
