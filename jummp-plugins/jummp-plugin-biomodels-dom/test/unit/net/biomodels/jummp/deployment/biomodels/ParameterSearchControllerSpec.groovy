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
        ParameterSearchCommand command = new ParameterSearchCommand()
        controller.index(command)
        then: "Should show correct view"
        String expectedView = "/parameterSearch/index"
        String actualView = view
        assert expectedView == actualView
    }

    void "test index with Query"() {
        given: "Controller and command object initialized"
        params.query = 'testQuery'
        params.size = 10
        params.start = 0
        params.sort = "model:ascending"
        when: "Redirected to index with query"
        controller.index()

        then: "Should show correct model"
        assert model.command.query == "testQuery"
        assert model.command.size == 10
        assert model.command.start == 0
        assert model.command.sort == "model:ascending"
    }

    void "test search with json format"() {
        given: "ParameterSearchService is mocked with certain values"
        def bindingMap = [query: "BIOMD*1", size: 10, start: 0, sort: "model:ascending"]
        ParameterSearchCommand command = new ParameterSearchCommand(bindingMap)


        def service = mockFor(ParameterSearchService)
        service.demand.getJSONData { ParameterSearchCommand cmd ->
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
        request.contentType = 'application/json'
        response.format="json"
        controller.search(command)

        then: "Result should contain correct results"
        response.json.recordsTotal == 2
        response.json.entries.first().fields.entity == "e1"

    }
    void "test search with xml format"() {
        given: "ParameterSearchService is mocked with certain values"
        def bindingMap = [query: "BIOMD*1", size: 10, start: 0, sort: "model:ascending"]
        ParameterSearchCommand command = new ParameterSearchCommand(bindingMap)


        def service = mockFor(ParameterSearchService)
        service.demand.getJSONData { ParameterSearchCommand cmd -> new ParameterSearchResults(recordsTotal: 2, recordsFiltered: 2,
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
        request.contentType = 'application/json'
        response.format="xml"
        controller.search(command)

        then: "Result should contain correct results"
        response.text.contains("<recordsTotal>2</recordsTotal>")
        response.text.contains("<entity>e1</entity>")
        response.text.contains("<entity>e2</entity>")


    }
    void "test search with xml format and no data (no matches found)"() {
        given: "ParameterSearchService is mocked with certain values"
        def bindingMap = [query: "NON_MATCHING_QUERY", size: 10, start: 0, sort: "model:ascending"]
        ParameterSearchCommand command = new ParameterSearchCommand(bindingMap)


        def service = mockFor(ParameterSearchService)
        service.demand.getJSONData { ParameterSearchCommand cmd ->
            new ParameterSearchResults(recordsTotal: 0, recordsFiltered: 0, entries:[])}
        controller.parameterSearchService = service.createMock()

        when : "Controller search method is invoked"
        request.contentType = 'application/json'
        response.format="xml"
        controller.search(command)

        then: "Result should contain correct results"
        response.text.contains("No matches found")
    }

    void "test search with csv format"() {
        given: "ParameterSearchService is mocked with certain values"
        def bindingMap = [query: "BIOMD*1", size: 10, start: 0, sort: "model:ascending"]
        ParameterSearchCommand command = new ParameterSearchCommand(bindingMap)


        def service = mockFor(ParameterSearchService)
        service.demand.getCSVData { ParameterSearchCommand cmd ->
           '"entity","reaction"\n'+
            '"e1","e1,e2=>e3"\n'+
            '"e2","e1,e2=>e3"'

        }
        controller.parameterSearchService = service.createMock()
        when : "Controller search method is invoked"
        request.contentType = 'application/json'
        response.format = "csv"
        controller.search(command)
        then: "Result should contain correct results"
        String[] responseArray =  response.text.split("\n")
        responseArray.length == 3
        responseArray[0].contains('"entity","reaction"')
        responseArray[1].contains('e1,e2=>e3')


    }
    void "test search with csv format with errenous parameters values"() {
        given: "ParameterSearchService is mocked with certain values"
        def bindingMap = [query: "NON_MATCHING_QUERY", size: 10, start: 400, sort: "model:ascending"]
        ParameterSearchCommand command = new ParameterSearchCommand(bindingMap)
        def service = mockFor(ParameterSearchService)
        service.demand.getCSVData { ParameterSearchCommand cmd -> throw new IOException("Unable to retrieve the data")}
        controller.parameterSearchService = service.createMock()
        when : "Controller search method is invoked"
        request.contentType = 'application/json'
        response.format = "csv"
        controller.search(command)
        then: "Result should contain correct results"
        response.text.contains("Unable to retrieve data")
    }


    void "test search with non matching format"() {
        given: "ParameterSearchService is mocked with certain values"
        def bindingMap = [query: "BIOMD*1", size: 10, start: 0, sort: "model:ascending"]
        ParameterSearchCommand command = new ParameterSearchCommand(bindingMap)


        def service = mockFor(ParameterSearchService)
        service.demand.getCSVData { ParameterSearchCommand cmd ->
           '"entity","reaction"\n'+
            '"e1","e1,e2=>e3"\n'+
            '"e2","e1,e2=>e3"'

        }
        controller.parameterSearchService = service.createMock()
        request.contentType = 'application/json'
        response.format="html"
        when : "Controller search method is invoked"
        controller.search(command)
        then: "Result should contain correct results"
        response.json.message == "Invalid format, please choose the format from JSON,XML and CSV"
        response.status == 415



    }
}
