package net.biomodels.jummp.deployment.biomodels

import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.services.ServiceUnitTestMixin
import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchCommand
import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchResults
import org.junit.Test
import org.junit.rules.ExpectedException
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(ParameterSearchService)
@TestMixin(ServiceUnitTestMixin)

class ParameterSearchServiceSpec extends Specification {

    ParameterSearchService parameterSearchService

    def setup() {
        parameterSearchService = new ParameterSearchService()
    }

    void "test ParameterSearchService positively"() {

        given: "A parameter search command object is defined with basic criteria"
        def bindingMap = [query: "BIOMD*1", size: 10, start: 0, sort: "entity:ascending"]
        ParameterSearchCommand command = new ParameterSearchCommand(bindingMap)

        when: "The service method's get data is called and parameter search command is valid"

        assert command.validate()
        ParameterSearchResults results = parameterSearchService.getData(command)

        then: "it should return correct number of records"

        int expectedTotalRecords = 3577
        int actualTotalRecords = results.recordsTotal
        assert expectedTotalRecords == actualTotalRecords

        then: "it should return correct records i.e. reaction value"

        String expectedReaction = "\\(glyceraldehyde 3\\-phosphate \\+ keto\\-D\\-fructose 6\\-phosphate\\) " +
            "=> \\(D\\-xylulose 5\\-phosphate \\+ 122357\\)"
        String actualReaction = results.entries.first().fields.reaction
        assert expectedReaction == actualReaction

        String expectedEntityId = "E4P"
        String actualEntityId = results.entries.first().fields.entity_id
        assert expectedEntityId == actualEntityId

        then: "it should return correct records i.e. model id"

        String expectedModel = "BIOMD0000000391"
        String actualModel = results.entries.first().fields.model
        assert expectedModel == actualModel

        then: "Number of records per page should be as per size value"

        int expectedEntriesSize = 10
        int actualEntriesSize = results.entries.size()
        assert expectedEntriesSize == actualEntriesSize


    }

    void "test ParameterSearchService Negatively"() {

        given: "A parameter search command object is defined with basic criteria"
        def bindingMap = [query: "NON_MATCHING_QUERY", size: 10, start: 0, sort: "entity:ascending"]
        ParameterSearchCommand command = new ParameterSearchCommand(bindingMap)

        when: "The service method's get data is called and parameter search command is valid"

        assert command.validate()
        ParameterSearchResults results = parameterSearchService.getData(command)

        then: "it should return correct number of records"

        int expectedTotalRecords = 0
        int actualTotalRecords = results.recordsTotal
        assert expectedTotalRecords == actualTotalRecords

    }

}
