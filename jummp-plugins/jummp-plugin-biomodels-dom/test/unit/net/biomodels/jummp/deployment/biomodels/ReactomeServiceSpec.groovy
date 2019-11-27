package net.biomodels.jummp.deployment.biomodels

import grails.test.mixin.TestFor

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(ReactomeService)
class ReactomeServiceSpec {

    def reactomeService

    void "test getPathwayForModelId"() {
        when: "Asked for reactome Id for BIOMD0000000255"

        def service = mockFor(ParameterSearchService)
        service.demand.getPathwayForModelId { String modelId ->
            return "R-HSA-1250196"
        }
        reactomeService = service.createMock()
        String reactomeId = reactomeService.getPathwayForModelId("BIOMD0000000255")
        then: "reactomeId string shouldn't be empty"
        !reactomeId.isEmpty() && null != reactomeId
    }
}
