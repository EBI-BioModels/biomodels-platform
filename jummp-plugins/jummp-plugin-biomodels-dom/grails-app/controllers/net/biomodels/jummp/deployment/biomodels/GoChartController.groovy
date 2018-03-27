package net.biomodels.jummp.deployment.biomodels

import com.fasterxml.jackson.databind.node.ArrayNode
import grails.plugin.springsecurity.annotation.Secured
import net.biomodels.jummp.core.model.ModelListSorting
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.models.ModelDetails
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicInteger

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

    private static final Logger LOGGER = LoggerFactory.getLogger(GoChartController.class)

    def index() {
        try {
            List data = modelService.getAllModelWithDetails(0, 0, true, ModelListSorting.ID)
            List<ModelDetails> models = data.collect{new ModelDetails(it[0] as Model, it[1] as String, it[3] as Date)}
            Map<?, ?> classified = modelClassifierService.classifyModels(models)
            ArrayNode converted = modelClassifierService.convertToJson(classified, new AtomicInteger(0))
            ['classifiedModels': converted]
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e)
            render(controller: "errors", action: "error500", plugin: "jummp-plugin-web-application")
        }
    }
}
