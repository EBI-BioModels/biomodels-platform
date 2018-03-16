package net.biomodels.jummp.deployment.biomodels

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import grails.plugin.springsecurity.annotation.Secured
import net.biomodels.jummp.core.model.ModelListSorting
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.models.JummpEntry
import net.biomodels.jummp.models.ModelDetails
import net.biomodels.jummp.models.RefData
import net.biomodels.jummp.utils.TimeUtils

@Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
class GoChartController {

    /**
     * Dependency Injection of ModelService
     */
    def modelService

    /**
     * Dependency Injection of ModelClassifierService
     */
    def modelClassifierService

    ObjectMapper objectMapper = new ObjectMapper()

    def index() {
        List<List<Model, String, String, Date, String, Long, String>> data =
            modelService.getAllModelWithDetails(0, 0, true, ModelListSorting.ID)
        List<ModelDetails> models = data.collect{new ModelDetails(it[0], it[1], it[3])}
        Map<?, ?> classified = modelClassifierService.classifyModels(models)
        ArrayNode converted = convertToJson(classified, new RefData<Integer>(0))
        ['classifiedModels': converted]
    }

    ArrayNode convertToJson(Map<?, ?> classified, RefData<Integer> refInt) {
        ArrayNode arrayNode = objectMapper.createArrayNode()
        for (JummpEntry<String, String> key : (classified.keySet() as List<JummpEntry<String, String>>)) {
            RefData<Integer> total = new RefData<>(0)
            ObjectNode node = objectMapper.createObjectNode()
            if (key.value != null) {
                node.put("name", key.value.replace("_", " "))
            } else {
                node.put("name", key.key)
            }
            node.put("code", key.key)
            if (classified.get(key) instanceof Map) {
                RefData<Integer> count = new RefData<>(0)
                node.putArray("children").addAll(convertToJson(classified.get(key) as Map<?, ?>, count))
                total.setData(total.getData() + count.data)
            } else {
                ArrayNode modelNodes = objectMapper.createArrayNode()

                for (ModelDetails model : (classified.get(key) as List<ModelDetails>)) {
                    total.data += 1
                    ObjectNode child = objectMapper.createObjectNode()
                    String modelId = model.model.getPublicationId()
                    if (modelId == null) {
                        modelId = model.model.submissionId
                    }
                    child.put("modelId", modelId)
                    child.put("name", model.name)
                    child.put("updateDate", TimeUtils.convertDate(model.updateDate, "yyyy-MM-dd"))
                    modelNodes.add(child)
                }
                node.putArray("models").addAll(modelNodes)
            }
            node.put("count", total.data)
            arrayNode.add(node)
            refInt.setData(refInt.data + total.data)
        }
        return arrayNode
    }
}
