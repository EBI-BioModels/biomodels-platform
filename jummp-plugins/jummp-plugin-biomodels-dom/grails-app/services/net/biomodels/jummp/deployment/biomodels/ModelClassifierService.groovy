package net.biomodels.jummp.deployment.biomodels

import com.fasterxml.jackson.core.type.TypeReference
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.models.JummpEntry
import net.biomodels.jummp.models.ModelDetails
import net.biomodels.jummp.utils.MathUtils
import net.biomodels.jummp.utils.RestUtils
import net.biomodels.jummp.utils.TimeUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.http.HttpMethod
import org.springframework.web.util.UriComponentsBuilder

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

    static final Logger LOGGER = LoggerFactory.getLogger(this.getClass())

    private String classificationEndpoint

    private Map<String, String> classifyModel(Model model) {
        LOGGER.debug("Starting classify model {}", model.submissionId)
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(classificationEndpoint)
        uriComponentsBuilder.path("/predict")
        uriComponentsBuilder.queryParam("model_id", model.getSubmissionId())
        URI request = uriComponentsBuilder.build().encode().toUri()
        Map<String, String> result = RestUtils.exchange(request, HttpMethod.GET,
            new TypeReference<HashMap<String, String>>(){})
        LOGGER.debug("Model {} classified result: {}", model.submissionId, result)
        return result
    }

    private Map<String, String> classifyModel(Model model, Date date) {
        JummpEntry<Long, Serializable> cache =
            cacheService.getCache(model.getSubmissionId()) as JummpEntry<Long, Serializable>
        if (cache != null) {
            /**
             * Check whether the model is updated or not
             */
            if (cache.key == TimeUtils.getTimestamp(date)) {
                return cache.getValue() as Map<String, String>
            }
        }

        Map<String, String> result = classifyModel(model)
        int expired = MathUtils.rand(TimeUtils.ONE_YEAR, TimeUtils.TWO_YEAR)
        cache = new JummpEntry<>(TimeUtils.getTimestamp(date), result as Serializable)
        cacheService.setCache(model.getSubmissionId(), cache, expired)
        return result
    }

    Map<?, ?> classifyModels(List<ModelDetails> models) {
        LOGGER.info("Starting to classify models")
        Map<?, ?> results = new HashMap<>()
        for (ModelDetails model : models) {
            Map<String, String> classified = classifyModel(model.model, model.updateDate)
            if (classified == null || classified.get("code") != "200") {
                continue
            }
            List<JummpEntry<String, String>> entries = new ArrayList<>()
            entries.add(new JummpEntry<>(classified.get("root_class"), classified.get("root_class_name")))
            entries.add(new JummpEntry<>(classified.get("parent_class"), classified.get("parent_class_name")))
            entries.add(new JummpEntry<>(classified.get("class"), classified.get("class_name")))
            classifyModels(results, entries.iterator(), model)
        }
        LOGGER.info("Finished classify models")
        return results
    }

    Map<?, ?> classifyModels(Map<?, ?> classified, Iterator<JummpEntry<String, String>> iterator, Object model) {
        JummpEntry<String, String> entry = iterator.next()
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

    void afterPropertiesSet() throws Exception {
        classificationEndpoint = grailsApplication.config.jummp.classification.endpoint
    }
}
