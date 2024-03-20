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
 * Spring Security (or a modified version of that library), containing parts
 * covered by the terms of Apache License v2.0, the licensors of this
 * Program grant you additional permission to convey the resulting work.
 * {Corresponding Source for a non-source form of such a combination shall
 * include the source code for the parts of Spring Security used as well as
 * that of the covered work.}
 **/


package net.biomodels.jummp.deployment.biomodels

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.models.ModelDetails
import net.biomodels.jummp.models.Progress
import net.biomodels.jummp.plugins.security.User
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.client.HttpStatusCodeException

@Secured(['ROLE_ADMIN'])
class ClassifierConfigureController {

    /**
     * Dependency Injection of ClassifierConfigureService
     */
    def classifierConfigureService

    /**
     * Dependency Injection of ModelClassifierService
     */
    def modelClassifierService

    /**
     * Dependency Injection of ModelService
     */
    def modelService

    /**
     * Dependency Injection of ModelClassService
     */
    def modelClassService

    /**
     * Dependency Injection of SpringSercurityService
     */
    def springSecurityService

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassifierConfigureController.class)

    @Secured(['ROLE_ADMIN', 'ROLE_CURATOR'])
    def index() {
        List data = modelService.getAllModelWithDetails(false)
        List<ModelDetails> modelDetailsList = data.collect {
            new ModelDetails(it[0] as Model, it[1] as String, it[2] as Date)
        }
        List<ModelClass> groundTruth = modelClassService.getModelClasses()
        List<Map<String, Object>> models = modelClassifierService.classifyAllModels(modelDetailsList, groundTruth)
        render(view: '/classifierConfiguration/configuration', model: [title: "Model Classification",
                 action: "saveGroundTruth", template: "classifierGroundTruth", models: models])
    }

    @Secured(['ROLE_ADMIN', 'ROLE_CURATOR'])
    def saveGroundTruth() {
        List<String> realCategories = params.list("realCategory[]")
        List<String> originalRealCategories = params.list("originalRealCategory[]")
        List<String> modelSubmissionIds = params.list("modelSubmissionId[]")
        for (int i = 0; i < modelSubmissionIds.size(); i++) {
            if (realCategories.get(i) != originalRealCategories.get(i)) {
                Model model = modelService.getModelBySubmissionId(modelSubmissionIds.get(i))
                if (realCategories.get(i) == "") {
                    modelClassService.deleteGroundTruth(model)
                } else {
                    User user = (User)springSecurityService.getCurrentUser()
                    modelClassService.saveGroundTruth(realCategories.get(i), model, user)
                }
            }
        }
        redirect(action: "index")
    }

    @Secured(['ROLE_ADMIN', 'ROLE_CURATOR'])
    def getPossibleCategories() {
        String submissionId = params.get("submission_id")
        response.status = 200
        try {
            List<Map<String, String>> possible = classifierConfigureService.getPossibleCategory(submissionId)
            render(new JSON(possible))
        } catch (HttpStatusCodeException e) {
            response.status = e.getStatusCode().value()
            render e.getResponseBodyAsString()
        }
    }

    @Secured(['ROLE_ADMIN', 'ROLE_CURATOR'])
    def searchCategories() {
        String keyword = params.get("keyword")
        response.status = 200
        try {
            List<Map<String, String>> possible = classifierConfigureService.searchCategory(keyword)
            render(new JSON(possible))
        } catch (HttpStatusCodeException e) {
            response.status = e.getStatusCode().value()
            render e.getResponseBodyAsString()
        }
    }

    def classifier = {
        List<Map<String, String>> dlModels = classifierConfigureService.getDLModels()
        render(view: '/classifierConfiguration/configuration',
            model: [title: "Model Classification", action: "switchModel", template: "classifier", dlModels: dlModels])
    }

    def classifierCreator = {
        render(view: '/classifierConfiguration/configuration', model: [title: "Model Classification Creator",
                 action: "classifierCreatorSave", template: "classifierCreator"])
    }

    def classifierCreatorSave = { DLModelCommand cmd ->
        if (!cmd.hasErrors()) {
            LOGGER.info("Creating DL model {}", cmd)
            List<Integer> hiddenLayers = new ArrayList<>()
            String hiddenLayerRequest = cmd.hiddenLayer
            if (hiddenLayerRequest != "Auto") {
                hiddenLayers = hiddenLayerRequest.split(",").collect{Integer.parseInt(it)}
            }
            try {
                classifierConfigureService.createDLModel(
                    cmd.dlName, cmd.totalEpoch, cmd.valPerEpoch, cmd.batchSize, hiddenLayers)
                redirect(action: "classifier")
            } catch (HttpStatusCodeException e) {
                LOGGER.error("An exception occurred when creating a new deep learning model, {}", e)
                flash.message = e.responseBodyAsString
            }
        }
        render(view: '/classifierConfiguration/configuration', model: [classifierCreator: cmd,
             title: "Model Classification Creator", action: "classifierCreatorSave", template: "classifierCreator"])
    }

    def delete = {
        String modelName = params.get("model_name")
        LOGGER.info("Deleting the Deep Learning model: {}", modelName)
        response.status = 200
        try {
            classifierConfigureService.deleteDLModel(modelName)
            render([message: message(code: 'modelclassifier.dllmodel.delete.success')] as JSON)
        } catch (HttpStatusCodeException e) {
            response.status = e.getStatusCode().value()
            render e.getResponseBodyAsString()
        }
    }

    def switchModel = {
        if (modelClassifierService.isRebuildingCache()) {
            response.status = 400
            render([message: message(code: "modelclassifier.cache.rebuild.message")] as JSON)
        } else {
            String modelName = params.get("activated")
            LOGGER.info("Switching to the Deep Learning model: {}", modelName)
            try {
                classifierConfigureService.switchDLModel(modelName)
                render([message: 'Operation success'] as JSON)
            } catch (HttpStatusCodeException e) {
                response.status = e.getStatusCode().value()
                render e.getResponseBodyAsString()
            }
        }
    }

    def rebuildCache = {
        if (modelClassifierService.isRebuildingCache()) {
            response.status = 400
            render([message: message(code: "modelclassifier.cache.rebuild.message")] as JSON)
        } else {
            List data = modelService.getAllModelWithDetails(false)
            List<ModelDetails> modelDetailsList = data.collect {
                new ModelDetails(it[0] as Model, it[1] as String, it[2] as Date)
            }
            modelClassifierService.rebuildClassifyCache(modelDetailsList)
            render([message: 'Operation success'] as JSON)
        }
    }

    def rebuildCacheStatus = {
        Progress progress = modelClassifierService.getCurrentTrainProgress()
        response.contentType = "application/json"
        render(new JSON(progress).toString())
    }

    def classifierDetails = {
        String modelName = params.get("model_name")
        DLModelCommand cmd = classifierConfigureService.getDLModel(modelName)

        render(view: '/classifierConfiguration/configuration', model: [title: "Model Classification Details",
             action: "retrainDLModel", template: "classifierDetails", classifierCreator: cmd])
    }

    def trainModelStatus = {
        String modelName = params.get("model_name")
        List<Map<String, String>> status = classifierConfigureService.getDLModelTrainStatus(modelName)
//        render(status.last() as JSON)
        render(new JSON(status.last()).toString())
    }

    def trainModelLogs = {
        String modelName = params.get("model_name")
        List<Map<String, String>> logs = classifierConfigureService.getDLModelTrainLogs(modelName)
        render(new JSON(logs).toString())
    }

    def retrainDLModel = { DLModelCommand cmd ->
        if (!cmd.hasErrors()) {
            LOGGER.info("Retraining the DL model: {}", cmd)
            List<Integer> hiddenLayers = new ArrayList<>()
            String hiddenLayerRequest = cmd.hiddenLayer
            if (hiddenLayerRequest != "Auto") {
                hiddenLayers = hiddenLayerRequest.split(",").collect{Integer.parseInt(it)}
            }
            try {
                classifierConfigureService.deleteDLModel(cmd.dlName)
                classifierConfigureService.createDLModel(
                    cmd.dlName, cmd.totalEpoch, cmd.valPerEpoch, cmd.batchSize, hiddenLayers)
                redirect(action: "classifier")
            } catch (HttpStatusCodeException e) {
                LOGGER.error("An exception occurred when creating a new deep learning model, {}", e)
                flash.message = e.responseBodyAsString
            }
        }
        render(view: '/classifierConfiguration/configuration', model: [classifierCreator: cmd,
             title: "Model Classification Creator", action: "retrainDLModel", template: "classifierDetails"])
    }
}
