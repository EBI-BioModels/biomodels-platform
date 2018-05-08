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
 */


package net.biomodels.jummp.deployment.biomodels

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.models.KV
import net.biomodels.jummp.models.ModelDetails
import net.biomodels.jummp.models.Progress
import net.biomodels.jummp.utils.MathUtils
import net.biomodels.jummp.core.util.RestUtils
import net.biomodels.jummp.utils.TimeUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.http.HttpMethod
import org.springframework.web.util.UriComponentsBuilder
import static grails.async.Promises.*

import java.util.concurrent.atomic.AtomicInteger

/**
 * @short: Service responsible for classify the bio-models into different categories
 * @author: Vu Tu <tvu@ebi.ac.uk>
 */
class ModelClassifierService implements InitializingBean {
    static transactional = false

    /**
     * Dependency Injection of CacheService
     */
    def cacheService
    /**
     * Dependency Injection of GrailsApplication
     */
    def grailsApplication

    /**
     * Dependency Injection of ObjectMapper
     */
    def objectMapper

    static final Logger LOGGER = LoggerFactory.getLogger(this.getClass())

    /**
     * Number of times that we will retry to call Classification API when it got an error
     * Out of this times, the service will raise that error
     */
    static final int RETRY_CLASSIFY_TIMES = 1

    private Progress trainProgress

    /**
     * Check if we are in rebuild cache process
     */
    private boolean inRebuildCacheProcess = false

    private static final String PROGRESS_CACHE_NAME = "ProgressCache"

    /**
     * Endpoint of the Classification API
     */
    private String classificationEndpoint

    void afterPropertiesSet() throws Exception {
        classificationEndpoint = grailsApplication.config.jummp.classification.endpoint
        trainProgress = cacheService.getCache(PROGRESS_CACHE_NAME)
        trainProgress = (trainProgress == null) ? new Progress(1, 0) : trainProgress
    }

    /**
     * Classify a model by make a request to Classification API
     * This method will raise an exception when it can't perform the request more than RETRY_CLASSIFY_TIMES
     * @param model: Model to classify
     * @return the HashMap represent the response from the Classification API
     */
    private Map<String, String> classifyModel(Model model) {
        LOGGER.debug("Starting classify model {}", model.submissionId)
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(classificationEndpoint)
        uriComponentsBuilder.path("/predict")
        uriComponentsBuilder.queryParam("model_id", model.getSubmissionId())
        URI request = uriComponentsBuilder.build().toUri()
        Map<String, String> result = RestUtils.exchange(request, HttpMethod.GET,
            new TypeReference<HashMap<String, String>>(){}, null, RETRY_CLASSIFY_TIMES, true)
        LOGGER.debug("Model {} classified result: {}", model.submissionId, result)
        return result
    }

    /**
     * Classify a model
     * Check if we already have cached, then return the cache instead of classify it again
     * @param model: Model to classify
     * @param date: The date that the model has been updated
     * @param forceUpdateCache: Alway update cache
     * @return the HashMap represent the response from the Classification API
     */
    private Map<String, String> classifyModel(Model model, Date date, boolean forceUpdateCache) {
        KV<Long, Serializable> cache =
            cacheService.getCache(model.getSubmissionId()) as KV<Long, Serializable>
        if (!forceUpdateCache && cache != null) {
            return cache.getValue() as Map<String, String>
        }

        Map<String, String> result = classifyModel(model)
        int expired = MathUtils.rand(TimeUtils.ONE_YEAR, TimeUtils.TWO_YEAR)
        cache = new KV<>(TimeUtils.getTimestamp(date), result as Serializable)
        cacheService.setCache(model.getSubmissionId(), cache, expired)
        return result
    }

    /**
     * Classify a list of models
     * @param models: List of models to classify
     * @return a Map that represent the tree of the classified result
     *
     * Example result:
     *  --Root
     *  ----Metabolic Process
     *  --------PhotoSynthesis
     *  ---------------Model 1
     *  ---------------Model 2
     *  ---------------Model 3
     *  ----Signalling
     *  --------Chemical synaptic transmission
     *  ---------------Model 4
     *  ---------------Model 5
     */
    private Map<?, ?> classifyModels(List<ModelDetails> models, boolean forceUpdateCache=false) {
        LOGGER.info("Starting to classify models")
        Map<?, ?> results = new HashMap<>()
        for (ModelDetails model : models) {
            Map<String, String> classified = classifyModel(model.model, model.updateDate, forceUpdateCache)
            if (forceUpdateCache) {
                trainProgress.increaseProgress()
                LOGGER.info("Rebuilding classify cache {}/{}", trainProgress.current, trainProgress.total)
                cacheService.setCache(PROGRESS_CACHE_NAME, trainProgress, TimeUtils.ONE_YEAR)
            }
            if (classified == null || classified.get("code") != "200") {
                continue
            }
            List<KV<String, String>> entries = new ArrayList<>()
            entries.add(new KV<>(classified.get("root_class"), classified.get("root_class_name")))
            entries.add(new KV<>(classified.get("parent_class"), classified.get("parent_class_name")))
            entries.add(new KV<>(classified.get("class"), classified.get("class_name")))
            classifyModels(results, entries.iterator(), model)
        }
        LOGGER.info("Finished classify models")
        return results
    }

    List<Map<String, String>> classifyAllModels(List<ModelDetails> modelDetails, List<ModelClass> groundTruth) {
        List<Map<String, String>> result = new ArrayList<>()
        for (ModelDetails model : modelDetails) {
            ModelClass modelClass = groundTruth.find{it.model == model.model}
            Map<String, String> classified = classifyModel(model.model, model.updateDate, false)
            if (classified == null || classified.get("code") != "200") {
                continue
            }
            Map<String, String> modelData = objectMapper.convertValue(model.model, Map.class)
            modelData.putAll(objectMapper.convertValue(model, Map.class))
            modelData.putAll(classified)
            modelData.put("groundTruth", modelClass == null ? "0": "1")
            modelData.put("realClass", modelClass == null ? "": modelClass.className)
            result.add(modelData)
        }
        return result
    }

    /**
     * Add the model to the right place in the result tree
     * @param classified: The Tree that represent the classified result
     * @param iterator: The List that represent each level of the classified model [root, parent, class]
     * @param model: The Model need to add
     */
    private Map<?, ?> classifyModels(Map<?, ?> classified, Iterator<KV<String, String>> iterator,
                                     Object model) {
        KV<String, String> entry = iterator.next()
        if (!iterator.hasNext()) {
            if (classified.containsKey(entry)) {
                (classified.get(entry) as List<Object>).add(model)
            } else {
                List<Object> allModels = new ArrayList<>()
                allModels.add(model)
                classified.put(entry, allModels)
            }
        }
        else if (classified.containsKey(entry)) {
            Map<?, ?> map = classified.get(entry) as Map<?, ?>
            classifyModels(map, iterator, model)
        } else {
            classified.put(entry, classifyModels(new HashMap<?, ?>(), iterator, model))
        }
        return classified
    }

    /**
     * Classify list of model
     * Also convert the result into ArrayNode (json)
     * @param models: List of models to classify
     * @return ArrayNode: object json represent the classified result tree
     */
    ArrayNode classify(List<ModelDetails> models, boolean forceUpdateCache=false) {
        Map<?, ?> classified = classifyModels(models, forceUpdateCache)
        convertToJson(classified, new AtomicInteger(0))
    }

    /**
     * Rebuild cache for classifier service
     * @param models: list of model to be rebuild
     */
    void rebuildClassifyCache(List<ModelDetails> models) {
        trainProgress = new Progress(models.size(), 0)
        inRebuildCacheProcess = true
        cacheService.setCache(PROGRESS_CACHE_NAME, trainProgress, TimeUtils.ONE_YEAR)
        def p = task {
            classifyModels(models, true)
        }
        p.onError {Throwable err ->
            LOGGER.error("Exception occurred during rebuild classify cache {}", err)
            inRebuildCacheProcess = false
        }
        p.onComplete {
            inRebuildCacheProcess = false
        }
    }

    /**
     * Check whether we are in rebuild cache process
     * @return
     */
    boolean isRebuildingCache() {
        return inRebuildCacheProcess
    }

    Progress getCurrentTrainProgress() {
        return trainProgress
    }

    /**
     * Convert the tree classified result from type of Map to type of ArrayNode (json)
     * Calculate the number models belonging each category
     * @param classified: The Map represent the tree classified result
     * @param totalCount: Start point (must be zero)
     * @return ArrayNode: object json represent the classified result tree
     */
    private ArrayNode convertToJson(Map<?, ?> classified, AtomicInteger totalCount) {
        ArrayNode arrayNode = objectMapper.createArrayNode()
        classified.each { KV<String, String> key, value ->
            AtomicInteger total = new AtomicInteger(0)
            ObjectNode node = objectMapper.createObjectNode()
            if (key.value != null) {
                node.put("name", key.value.replace("_", " "))
            } else {
                node.put("name", key.key)
            }
            node.put("code", key.key)
            if (value instanceof Map) {
                AtomicInteger count = new AtomicInteger(0)
                node.putArray("children").addAll(convertToJson(value as Map<?, ?>, count))
                total.set(total.get() + count.get())
            } else {
                ArrayNode modelNodes = objectMapper.createArrayNode()

                for (ModelDetails model : (value as List<ModelDetails>)) {
                    total.incrementAndGet()
                    ObjectNode child = objectMapper.createObjectNode()
                    String modelId = model.model.getPublicationId()
                    if (modelId == null) {
                        modelId = model.model.submissionId
                    }
                    child.put("modelId", modelId)
                    child.put("name", model.name)
                    child.put("updateDate", model.updateDate.format("yyyy-MM-dd"))
                    modelNodes.add(child)
                }
                node.putArray("models").addAll(modelNodes)
            }
            node.put("count", total.get())
            arrayNode.add(node)
            totalCount.set(totalCount.get() + total.get())
        }
        return arrayNode
    }
}
