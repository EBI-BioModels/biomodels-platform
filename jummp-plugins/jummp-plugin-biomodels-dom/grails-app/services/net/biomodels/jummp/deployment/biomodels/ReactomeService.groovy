package net.biomodels.jummp.deployment.biomodels

import grails.transaction.Transactional
import grails.util.Holders
import net.biomodels.jummp.models.ReactomeMapper

@Transactional
class ReactomeService {

    def grailsApplication = Holders.grailsApplication.mainContext.getBean('grailsApplication')

    Map<String,String> getModelPathwayMapFromReactomeFactoryBean() {
        return grailsApplication?.mainContext?.reactomeMapperFactoryBean?.getModelPathwayMap()
    }

    String getPathwayForModelId(String modelId) {
        Map<String, String> modelPathwayMap  = getModelPathwayMapFromReactomeFactoryBean()
        return modelPathwayMap.get(modelId)
    }
}
