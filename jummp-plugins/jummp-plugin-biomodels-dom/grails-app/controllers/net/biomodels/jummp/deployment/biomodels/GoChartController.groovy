package net.biomodels.jummp.deployment.biomodels

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import grails.plugin.springsecurity.annotation.Secured
import net.biomodels.jummp.core.model.ModelListSorting
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.models.JummpEntry
import net.biomodels.jummp.models.ModelDetails
import net.biomodels.jummp.models.RefData

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


    def index() {
        List data = modelService.getAllModelWithDetails(0, 0, true, ModelListSorting.ID)
        List<ModelDetails> models = data.collect{new ModelDetails(it[0] as Model, it[1] as String, it[3] as Date)}
        Map<?, ?> classified = modelClassifierService.classifyModels(models)
        ArrayNode converted = convertToJson(classified, new RefData<Integer>(0))
        ['classifiedModels': converted]
    }

        }
        return arrayNode
    }
}
