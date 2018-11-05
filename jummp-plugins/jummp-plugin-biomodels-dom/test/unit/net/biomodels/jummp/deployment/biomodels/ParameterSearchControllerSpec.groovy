package net.biomodels.jummp.deployment.biomodels

import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchCommand
import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchResults
import net.biomodels.jummp.deployment.biomodels.parameters.SearchResultEntry
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */

@TestFor(ParameterSearchController)
@TestMixin(GrailsUnitTestMixin)
class ParameterSearchControllerSpec extends Specification {

    ParameterSearchService parameterSearchService
    ParameterSearchController controller

    def setup() {
        parameterSearchService = new ParameterSearchService()
        controller = new ParameterSearchController()
    }

    void "test index"() {
        given: "Controller and command initialized"
        when: "Redirected to index"
        controller.index()
        then: "Should show correct view"
        String expectedView = "/parameterSearch/index"
        String actualView = view
        assert expectedView == actualView
    }

    void "test search"() {
        given: "ParameterSearchService is mocked with certain values"
        def bindingMap = [query: "BIOMD*1", size: 10, start: 0, sort: "entity:ascending"]
        ParameterSearchCommand command = new ParameterSearchCommand(bindingMap)


        def service = mockFor(ParameterSearchService)
        service.demand.getData { ParameterSearchCommand cmd ->
            new ParameterSearchResults(recordsTotal: 2, recordsFiltered: 2,
                entries: [
                    new SearchResultEntry(fields: [
                        entity: 'e1'
                    ]),
                    new SearchResultEntry(fields: [
                        entity: 'e2'
                    ])
                ]
            )

        }
        controller.parameterSearchService = service.createMock()
        when : "Controller search method is invoked"
        controller.search(command)


        then: "Result should contain correct results"
        assert response.json.recordsTotal == 2
        assert response.json.entries.first().fields.entity == "e1"

    }
}
