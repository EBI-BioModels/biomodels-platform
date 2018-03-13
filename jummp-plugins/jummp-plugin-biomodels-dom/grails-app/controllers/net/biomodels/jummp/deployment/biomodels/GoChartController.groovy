package net.biomodels.jummp.deployment.biomodels

import grails.plugin.springsecurity.annotation.Secured
import net.biomodels.jummp.core.model.ModelListSorting
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.models.JummpEntry

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
        List<List<Model, String, Date, String, Long, String>> data =
            modelService.getAllModelWithDetails(0, 0, true, ModelListSorting.ID)
        List<JummpEntry<Model, Date>> models = data.collect{new JummpEntry<>(it[0], it[3])}
        Map<?, ?> classified = modelClassifierService.classifyModels(models)
        ['classifiedModels': classified]
    }
}
