package net.biomodels.jummp.deployment.biomodels

import com.google.gson.JsonObject
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import grails.test.runtime.FreshRuntime
import net.biomodels.jummp.core.util.RestUtils
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.models.KV
import net.biomodels.jummp.models.ModelDetails
import org.codehaus.groovy.grails.commons.InstanceFactoryBean
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import spock.lang.Specification

@FreshRuntime
@TestMixin(GrailsUnitTestMixin)
/*
 * @MockFor(ModelClassifierService) does not initialise the modelClassifierService bean properly
 * and its afterPropertiesSet() method does not seem to be called.
 * We therefore manually define the bean using doWithSpring and instantiate it in setup()
 */
class ModelClassifierServiceSpec extends Specification {
    def service
    def cacheService = Mock(CacheService) {
        hasCache() >> false
        getCache() >> null
        setCache() >> null
    }

    def doWithConfig(c) {
        c.jummp.classification.endpoint = "http://127.0.0.1:5000"
    }

    def doWithSpring = {
        modelClassifierService(ModelClassifierService) { bean ->
            grailsApplication = ref('grailsApplication')
            cacheService      = ref('cacheService')
        }
        cacheService(InstanceFactoryBean, cacheService, CacheService)
    }

    def setup() {
        service = getGrailsApplication().mainContext.modelClassifierService
    }

    void "test single model classify result parser"() {
        given:
            JsonObject response = new JsonObject()
            response.addProperty("class", "GO:0007165")
            response.addProperty("class_name", "signal transduction")
            response.addProperty("parent_class", "GO:0023052")
            response.addProperty("parent_class_name", "signaling")
            response.addProperty("root_class", "GO:0008150")
            response.addProperty("root_class_name", "biological_process")
            response.addProperty("code", 200)
            String modelToTest = "MODEL7519354389"
            def restTemplate = mockFor(RestTemplate)
            restTemplate.demand.exchange { URI uri, HttpMethod httpMethod, Object object, Class aClass
                -> return new ResponseEntity<String>(response.toString(), HttpStatus.OK)}
            RestUtils.restTemplate = restTemplate.createMock()
        when:
            Model testModel = new Model()
            testModel.setSubmissionId(modelToTest)
            List<ModelDetails> models = new ArrayList<>()
            models.add(new ModelDetails(testModel, "Sample Model For Testing", new Date()))
            Map<?, ?> result = service.classifyModels(models)
        then:
            1 == result.keySet().size()
            KV<String, String> rootKey = new KV<>("GO:0008150", "biological_process")
            result.containsKey(rootKey)
            KV<String, String> parentKey = new KV<>("GO:0023052", "signaling")
            (result.get(rootKey) as Map).containsKey(parentKey)
            KV<String, String> classKey = new KV<>("GO:0007165", "signal transduction")
            ((result.get(rootKey) as Map).get(parentKey) as Map).containsKey(classKey)
            List<ModelDetails> classified = (((result.get(rootKey) as Map).get(parentKey) as Map).get(classKey) as List)
            1 == classified.size()
            classified.get(0).model.submissionId == modelToTest
    }

    String getModelIdFromURI(URI uri) {
        return uri.toString().split("model_id=")[1]
    }

    Model createModel(String submissionId) {
        Model model = new Model()
        model.setSubmissionId(submissionId)
        return model
    }

    void "test multiple model classify result parser"() {
        given:
            Map<String, JsonObject> responseMock = new HashMap<>();
            JsonObject firstModelResponse = new JsonObject()
            firstModelResponse.addProperty("class", "GO:0007165")
            firstModelResponse.addProperty("class_name", "signal transduction")
            firstModelResponse.addProperty("parent_class", "GO:0023052")
            firstModelResponse.addProperty("parent_class_name", "signaling")
            firstModelResponse.addProperty("root_class", "GO:0008150")
            firstModelResponse.addProperty("root_class_name", "biological_process")
            firstModelResponse.addProperty("code", 200)
            responseMock.put("MODEL7519354389", firstModelResponse)

            JsonObject secondModelResponse = new JsonObject()
            secondModelResponse.addProperty("class", "GO:0007165")
            secondModelResponse.addProperty("class_name", "signal transduction")
            secondModelResponse.addProperty("parent_class", "GO:0023052")
            secondModelResponse.addProperty("parent_class_name", "signaling")
            secondModelResponse.addProperty("root_class", "GO:0008150")
            secondModelResponse.addProperty("root_class_name", "biological_process")
            secondModelResponse.addProperty("code", 200)
            responseMock.put("MODEL7743608569", secondModelResponse)

            JsonObject thirdModelResponse = new JsonObject()
            thirdModelResponse.addProperty("class", "GO:0042221")
            thirdModelResponse.addProperty("class_name", "response to chemical")
            thirdModelResponse.addProperty("parent_class", "GO:0050896")
            thirdModelResponse.addProperty("parent_class_name", "response to stimulus")
            thirdModelResponse.addProperty("root_class", "GO:0008150")
            thirdModelResponse.addProperty("root_class_name", "biological_process")
            thirdModelResponse.addProperty("code", 200)
            responseMock.put("MODEL8102792069", thirdModelResponse)
            def restTemplate = mockFor(RestTemplate)
            restTemplate.demand.exchange(1..3) { URI uri, HttpMethod httpMethod, Object object, Class aClass
                -> return new ResponseEntity<String>(
                            responseMock.get(getModelIdFromURI(uri)).toString(), HttpStatus.OK)}
            RestUtils.restTemplate = restTemplate.createMock()
        when:
            List<ModelDetails> models = responseMock.keySet().collect{
                new ModelDetails(createModel(it), "Sample Model For Testing", new Date())}
            Map<?, ?> result = service.classifyModels(models)
        then:
            1 == result.keySet().size()
            KV<String, String> rootKey = new KV<>("GO:0008150", "biological_process")
            result.containsKey(rootKey)
            2 == (result.get(rootKey) as Map).keySet().size()
            KV<String, String> parentKey = new KV<>("GO:0023052", "signaling")
            (result.get(rootKey) as Map).containsKey(parentKey)
            1 == ((result.get(rootKey) as Map).get(parentKey) as Map).size()
            KV<String, String> classKey = new KV<>("GO:0007165", "signal transduction")
            ((result.get(rootKey) as Map).get(parentKey) as Map).containsKey(classKey)
            List<Model> classified = (((result.get(rootKey) as Map).get(parentKey) as Map).get(classKey) as List)
            2 == classified.size()
            KV<String, String> thirdParentKey = new KV<>("GO:0050896", "response to stimulus")
            (result.get(rootKey) as Map).containsKey(thirdParentKey)

    }
}
