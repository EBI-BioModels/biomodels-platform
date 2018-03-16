package net.biomodels.jummp.deployment.biomodels

import com.fasterxml.jackson.core.type.TypeReference
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.models.JummpEntry
import net.biomodels.jummp.models.ModelDetails
import net.biomodels.jummp.utils.MathUtils
import net.biomodels.jummp.utils.RestUtils
import net.biomodels.jummp.utils.TimeUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
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

    static final Log log = LogFactory.getLog(this.getClass())

    private String classificationEndpoint

    private Map<String, String> classifyModel(Model model) {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(classificationEndpoint)
        uriComponentsBuilder.path("/predict")
        uriComponentsBuilder.queryParam("model_id", model.getSubmissionId())
        URI request = uriComponentsBuilder.build().encode().toUri()
        return  RestUtils.exchange(request, HttpMethod.GET, new TypeReference<HashMap<String, String>>(){})
    }

    private Map<String, String> classifyModel(Model model, Date date) {
        if (cacheService.hasCache(model.getSubmissionId())) {
            JummpEntry<Long, Serializable> cache =
                cacheService.getCache(model.getSubmissionId()) as JummpEntry<Long, Serializable>
            /**
             * Check whether the model is updated or not
             */
            if (cache.key == TimeUtils.getTimestamp(date)) {
                return cache.getValue() as Map<String, String>
            }
        }

        Map<String, String> result = classifyModel(model)
        int expired = MathUtils.rand(TimeUtils.ONE_YEAR, TimeUtils.TWO_YEAR)
        JummpEntry<Long, Serializable> cache = new JummpEntry<>(TimeUtils.getTimestamp(date), result as Serializable)
        cacheService.setCache(model.getSubmissionId(), cache, expired)
        return result
    }

    Map<?, ?> classifyModels(List<ModelDetails> models) {
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
