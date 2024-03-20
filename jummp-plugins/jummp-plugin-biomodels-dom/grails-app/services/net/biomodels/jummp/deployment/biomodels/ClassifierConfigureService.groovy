/**
 * Copyright (C) 2010-20204 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

import com.fasterxml.jackson.core.type.TypeReference
import net.biomodels.jummp.core.util.RestUtils
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.web.util.UriComponentsBuilder

/**
 * @short: Service responsible for classifier configure
 *
 * @author: Vu Tu <tvu@ebi.ac.uk>
 */
class ClassifierConfigureService implements InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassifierConfigureService.class)

    static transactional = false

    def grailsApplication

    /**
     * Endpoint of the Classification API
     */
    private String classificationEndpoint

    /**
     * Number of times that we will retry to call Classification API when it got an error
     * Out of this times, the service will raise that error
     */
    static final int RETRY_CLASSIFY_TIMES = 3

    private HttpEntity requestEntity

    void afterPropertiesSet() throws Exception {
        // http headers
        HttpHeaders headers = new HttpHeaders()
        // set `Content-Type` and `Accept` headers
        headers.setContentType(MediaType.APPLICATION_JSON)
        requestEntity = new HttpEntity(headers)

        classificationEndpoint = grailsApplication.config.jummp.classification.endpoint
        LOGGER.info("Finished the bean initialisation")
    }

    /**
     * Create a new Deep learning model
     * @param name: the name of this deep learning model, must be unique
     * @param totalEpoch: total number of epoch to train
     * @param valPerEpoch: number of validate during train
     * @param batchSize: number of bio-models train in one batch
     * @param hiddenLayers: List of layers and number neurons in each layer
     */
    void createDLModel(String name, int totalEpoch, int valPerEpoch, int batchSize, List<Integer> hiddenLayers) {
        String output = ""
        JSONObject jsonObject = new JSONObject()
        try {
            jsonObject.put("workspace", name)
            jsonObject.put("total_epoch", Integer.toString(totalEpoch))
            jsonObject.put("val_per_epoch", Integer.toString(valPerEpoch))
            jsonObject.put("batch_size", Integer.toString(batchSize))
            if (hiddenLayers.size() > 0) {
                jsonObject.put("hidden_layer", hiddenLayers.join(","))
            }
            output = RestUtils.sendPost("$classificationEndpoint/train", jsonObject)
        } catch (Exception e) {
            LOGGER.error("An error occurred when creating a Deep Learning model with these params: ${jsonObject}")
            e.printStackTrace()
        } finally {
            LOGGER.info("The output of creating a new trained model: $output")
        }
    }

    /**
     * Set the classifier service to switch to a other exists DL model
     * @param modelName: The name of the DL model to switch
     */
    void switchDLModel(String modelName) {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(classificationEndpoint)
        uriComponentsBuilder.path("/workspace")
        uriComponentsBuilder.queryParam("workspace", modelName)
        URI uri = uriComponentsBuilder.build().toUri()
        RestUtils.exchange(uri, HttpMethod.PUT, new TypeReference<String>() {}, null, 1)
    }

    /**
     * Delete a DL model
     * @param modelName: The name of the DL model to delete
     */
    void deleteDLModel(String modelName) {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(classificationEndpoint)
        uriComponentsBuilder.path("/workspace")
        uriComponentsBuilder.queryParam("workspace", modelName)
        URI uri = uriComponentsBuilder.build().toUri()
        RestUtils.exchange(uri, HttpMethod.DELETE, new TypeReference<String>() {}, null, 1)
    }

    /**
     * Get list of DL models with some basics details
     * @return List models
     */
    List<Map<String, String>> getDLModels() {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(classificationEndpoint)
        uriComponentsBuilder.path("/workspace")
        URI request = uriComponentsBuilder.build().toUri()
        List<Map<String, String>> workspaces = RestUtils.exchange(request, HttpMethod.GET,
            new TypeReference<List<HashMap<String, String>>>(){}, null, RETRY_CLASSIFY_TIMES)
        for (Map<String, String> workspace : workspaces) {
            List<Map<String, String>> status = getDLModelTrainStatus(workspace.get("name"))
            workspace.putAll(status.last())
            Date date = new Date(Float.parseFloat(workspace['create_time']).longValue() * 1000L)
            workspace.put('create_date', date.format("yyyy - MMM dd - HH:mm"))
        }
        return workspaces
    }

    /**
     * Get deep learning model details
     * @param modelName
     * @return
     */
    DLModelCommand getDLModel(String modelName) {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(classificationEndpoint)
        uriComponentsBuilder.path("/workspace")
        uriComponentsBuilder.queryParam("workspace", modelName)
        URI request = uriComponentsBuilder.build().toUri()
        Map<String, String> modelDetails = RestUtils.exchange(request, HttpMethod.GET,
            new TypeReference<HashMap<String, String>>(){}, null, RETRY_CLASSIFY_TIMES)
        DLModelCommand dlModelCommand = new DLModelCommand()
        dlModelCommand.setDlName(modelName)
        dlModelCommand.setBatchSize(Integer.parseInt(modelDetails['batch_size']))
        dlModelCommand.setTotalEpoch(Integer.parseInt(modelDetails['total_epoch']))
        dlModelCommand.setValPerEpoch(Integer.parseInt(modelDetails['val_per_epoch']))
        String hiddenLayer = modelDetails['hidden_layer'] == null ? "Auto" : modelDetails['hidden_layer']
        dlModelCommand.setHiddenLayer(hiddenLayer)
        return dlModelCommand
    }

    /**
     * Get deep learning model train status
     * @param modelName
     * @return
     */
    List<Map<String, String>> getDLModelTrainStatus(String modelName) {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(classificationEndpoint)
        uriComponentsBuilder.path("/train")
        uriComponentsBuilder.queryParam("workspace", modelName)
        URI request = uriComponentsBuilder.build().toUri()
        return RestUtils.exchange(request, HttpMethod.GET,
            new TypeReference<List<HashMap<String, String>>>(){}, requestEntity, RETRY_CLASSIFY_TIMES)
    }

    /**
     * Get deep learning model train logs
     * @param modelName
     * @return
     */
    List<Map<String, String>> getDLModelTrainLogs(String modelName) {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(classificationEndpoint)
        uriComponentsBuilder.path("/dl")
        uriComponentsBuilder.queryParam("workspace", modelName)
        URI request = uriComponentsBuilder.build().toUri()
        return RestUtils.exchange(request, HttpMethod.GET,
            new TypeReference<List<HashMap<String, String>>>(){}, requestEntity, RETRY_CLASSIFY_TIMES)
    }

    /**
     * Get possible category for specific model
     * @param submissionId
     * @return
     */
    List<Map<String, String>> getPossibleCategory(String submissionId) {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(classificationEndpoint)
        uriComponentsBuilder.path("/possible")
        uriComponentsBuilder.queryParam("model_id", submissionId)
        URI request = uriComponentsBuilder.build().toUri()
        return RestUtils.exchange(request, HttpMethod.GET,
            new TypeReference<List<HashMap<String, String>>>(){}, requestEntity, RETRY_CLASSIFY_TIMES)
    }

    /**
     * Search ontology based on GO tree
     * @param keyword
     * @return
     */
    List<Map<String, String>> searchCategory(String keyword) {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(classificationEndpoint)
        uriComponentsBuilder.path("/ontology/search")
        uriComponentsBuilder.queryParam("keyword", keyword)
        URI request = uriComponentsBuilder.build().toUri()
        return RestUtils.exchange(request, HttpMethod.GET,
            new TypeReference<List<HashMap<String, String>>>(){}, requestEntity, RETRY_CLASSIFY_TIMES)
    }
}
