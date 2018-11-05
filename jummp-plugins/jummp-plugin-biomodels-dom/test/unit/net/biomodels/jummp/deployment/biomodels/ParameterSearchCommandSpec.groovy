package net.biomodels.jummp.deployment.biomodels

import grails.test.mixin.TestMixin
import grails.test.mixin.services.ServiceUnitTestMixin
import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchCommand
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestMixin(ServiceUnitTestMixin)

class ParameterSearchCommandSpec extends Specification {

    void "test ParameterSearchCommand Positively"() {

        given: "A parameter search command object is defined with basic criteria"
        def bindingMap = [query: "E4P*", size: 10, start: 0, sort: "entity:ascending"]
        ParameterSearchCommand command = new ParameterSearchCommand(bindingMap)

        when: "When command is validated with positive criteria"

        then: "Validation should return true"
        assert command.validate()
    }


    void "test ParameterSearchCommand Negatively"() {

        given: "A parameter search command object is defined with negative criteria"
        def bindingMap = [query: "E4P*", size: 13, start: -5, sort: "entity:ascending"]
        ParameterSearchCommand command = new ParameterSearchCommand(bindingMap)

        when: "When command is validated with negative criteria"

        then: "Validation should return false"
        assert !command.validate()
    }

    void "test ParameterSearchCommand URL"() {

        given: "A parameter search command object is defined with negative criteria"
        def bindingMap = [query: "E4P*", size: 10, start: 0, sort: "entity:ascending"]
        ParameterSearchCommand command = new ParameterSearchCommand(bindingMap)

        when: "When command is validated with positive criteria"

        then: "Validation should return true"
        assert command.validate()

        then : "It should form correct url"

        String expectedSearchUrl = "https://wwwdev.ebi.ac.uk/ebisearch/ws/rest/biomodels_parameters?format=json" +
            "&fields=entity,entity_id,reaction,model,publication,rate,parameters" +
            "&query=E4P*&size=10&start=0&sort=entity:ascending"
        String actualSearchUrl = command.getSearchUrl()

        assert expectedSearchUrl == actualSearchUrl
    }
}
