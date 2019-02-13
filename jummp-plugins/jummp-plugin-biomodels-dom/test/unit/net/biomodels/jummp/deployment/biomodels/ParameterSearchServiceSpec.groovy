package net.biomodels.jummp.deployment.biomodels

import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.services.ServiceUnitTestMixin
import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchCommand
import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchResults
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(ParameterSearchService)
@TestMixin(ServiceUnitTestMixin)

class ParameterSearchServiceSpec extends Specification {

    void "test ParameterSearchService positively"() {

        given: "A parameter search command object is defined with basic criteria"
        def bindingMap = [query: "BIOMD0000000292", size: 10, start: 0, sort: "entity:ascending"]
        ParameterSearchCommand command = new ParameterSearchCommand(bindingMap)

        when: "The service method's get data is called and parameter search command is valid"
        command.validate()
        ParameterSearchResults results = service.getJSONData(command)

        then: "it should return correct number of records"
        results.recordsTotal > 0

        and: "it should return correct records"

        String expectedReaction = "([3-phospho-D-glyceric acid, D-ribulose 1,5-bisphosphate, 3-Phospho-D-glycerate, " +
            "D-Ribulose 1,5-bisphosphate] + [CHEBI:16474, C00005] " +
            "+ [CHEBI:15422, C00002]) => " +
            "([CHEBI:16108, aldehydo-D-ribose 5-phosphate, D-ribulose 5-phosphate, keto-D-fructose 1,6-bisphosphate, " +
            "keto-D-fructose 6-phosphate, D-erythrose 4-phosphate, sedoheptulose 1,7-bisphosphate, sedoheptulose 7-phosphate, " +
            "D-glyceraldehyde 3-phosphate, C00111, D-Ribose 5-phosphate;, D-Ribulose 5-phosphate, Sedoheptulose 7-phosphate;, " +
            "Sedoheptulose 1,7-bisphosphate, D-Erythrose 4-phosphate, C00085, C00354, C00118] + [C00008, CHEBI:16761] + [NADP(+), C00006])"
        String expectedEntityId = "Y"
        String expectedModel = "BIOMD0000000292"


        boolean isTestPassed = false

        results.entries.each{value ->
            if(value.fields.reaction == expectedReaction && value.fields.entity_id == expectedEntityId && value.fields.model == expectedModel) {
                isTestPassed = true
                return true
            }
        }
        isTestPassed
        and: "Number of records per page should be as per size value"
        10 == results.entries.size()


    }

    void "test ParameterSearchService Negatively"() {

        given: "A parameter search command object is defined with basic criteria"
        def bindingMap = [query: "NON_MATCHING_QUERY", size: 10, start: 0, sort: "entity:ascending"]
        ParameterSearchCommand command = new ParameterSearchCommand(bindingMap)

        when: "The service method's get data is called and parameter search command is valid"

        command.validate()
        ParameterSearchResults results = service.getJSONData(command)

        then: "it should return correct number of records"
        0 == results.recordsTotal

    }

    void "test ParameterSearchService With CSV data"() {

        given: "A parameter search command object is defined with basic criteria"
        def bindingMap = [query: "BIOMD0000000292", size: 10, start: 0, sort: "entity:ascending"]
        ParameterSearchCommand command = new ParameterSearchCommand(bindingMap)

        when: "The service method's get data is called and parameter search command is valid"

        command.validate()
        String results = service.getCSVData(command)

        then: "it should return correct number of records"
        results.indexOf("[C00008, CHEBI:16761]") != -1
        results.indexOf("entity_RAW") == -1
        results.indexOf("reaction_RAW") == -1

    }

    void "test ParameterSearchService With XML data"() {

        given: "A parameter search command object is defined with basic criteria"
        def bindingMap = [query: "BIOMD0000000292", size: 10, start: 0, sort: "entity:ascending"]
        ParameterSearchCommand command = new ParameterSearchCommand(bindingMap)

        when: "The service method's get data is called and parameter search command is valid"

        command.validate()
        String results = service.getXMLData(command)

        then: "it should return correct number of records"
        results.indexOf("<value>[C00008, CHEBI:16761]</value>") != -1
        results.indexOf("entity_RAW") == -1
        results.indexOf("reaction_RAW") == -1

    }

}
