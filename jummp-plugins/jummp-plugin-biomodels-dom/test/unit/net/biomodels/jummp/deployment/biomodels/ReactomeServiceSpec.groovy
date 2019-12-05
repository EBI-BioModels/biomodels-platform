package net.biomodels.jummp.deployment.biomodels

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import net.biomodels.jummp.models.ReactomeServiceFactoryBean

/**
 * @author carankalle on 26/11/2019.
 * @author mglont on 05/12/2019.
 */
@TestMixin(GrailsUnitTestMixin)
class ReactomeServiceSpec {
    private final String reactomeId = "R-HSA-1250196"
    private final String modelId = "BIOMD0000000255"
    private final Map<String, String> reactomeMapping = [
        modelId: reactomeId
    ]

    def doWithSpring = {
        reactomeService(DummyReactomeServiceFactoryBean, reactomeMapping) {
            it.scope = 'prototype'
        }
    }
    void "test getPathwayForModelId"() {

        when: "Asked for reactome Id for a known model"
        def service = grailsApplication.mainContext.reactomeService

        String actualReactomeId = service.getPathwayForModelId(modelId)
        then: "reactomeId string shouldn't be empty"
        actualReactomeId == reactomeId

        and: "asking for a pathway id for a model that does not exist should return null"
        service.getPathwayForModelId("THIS-DOES-NOT-EXIST") == null
        service.getPathwayForModelId("") == null
        service.getPathwayForModelId(null) == null
    }
}

class DummyReactomeServiceFactoryBean extends ReactomeServiceFactoryBean {
    final Map<String, String> mockModelPathwayMapping

    DummyReactomeServiceFactoryBean(Map<String, String> mapping) {
        this.mockModelPathwayMapping = mapping
    }

    @Override
    ReactomeService getObject() throws Exception {
        new ReactomeService(mockModelPathwayMapping)
    }
}
