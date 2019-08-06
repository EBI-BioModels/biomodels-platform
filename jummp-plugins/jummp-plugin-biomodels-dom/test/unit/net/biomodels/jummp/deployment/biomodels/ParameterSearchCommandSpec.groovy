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
        command.validate()
    }


    void "test ParameterSearchCommand Negatively with size and start"() {

        given: "A parameter search command object is defined with negative criteria"
        def bindingMap = [query: "E4P*", size: 13, start: -5, sort: "entity:ascending"]
        ParameterSearchCommand command = new ParameterSearchCommand(bindingMap)

        when: "When command is validated with negative criteria"
        then: "Validation should return false"
        !command.validate()
    }

    void "test ParameterSearchCommand positively with query = *"() {

        given: "A parameter search command object is defined with negative criteria"
        def bindingMap = [query: null, size: 10, start: 0, sort: "entity:ascending"]
        ParameterSearchCommand command = new ParameterSearchCommand(bindingMap)

        when: "When command is validated with negative criteria"

        then: "Validation should return false"
        command.validate()
        assertEquals("*:*",command.query)
    }


    void "test ParameterSearchCommand URL"() {

        given: "A parameter search command object is defined with negative criteria"
        def bindingMap = [query: "E4P*", size: 10, start: 0, sort: "entity:ascending"]
        ParameterSearchCommand command = new ParameterSearchCommand(bindingMap)

        when: "When command is validated with positive criteria"
        then: "Validation should return true"
        command.validate()

        and : "It should form correct url"


        String expectedSearchUrl = "https://wwwdev.ebi.ac.uk/ebisearch/ws/rest/biomodels_parameters?" +
            "fields=entity_RAW,entity_id,initial_data_RAW,reaction_RAW,reaction_original_RAW,model,organism,publication," +
                "rate_RAW,rate_original_RAW,parameters_RAW,entity_accession_url,reaction_sbo_term_link,entity_sbo_term_link,external_links" +
            "&query=E4P*&size=10&start=0&sort=entity:ascending&format=json"
        String actualSearchUrl = command.getSearchUrl("json")
        expectedSearchUrl == actualSearchUrl
    }

    void "test ParameterSearchCommand URL with special character for accession"() {

        given: "A parameter search command object is defined with negative criteria"
        def bindingMap = [query: "GO:0005892", size: 10, start: 0, sort: "entity:ascending"]
        ParameterSearchCommand command = new ParameterSearchCommand(bindingMap)

        when: "When command is validated with positive criteria"
        then: "Validation should return true"
        command.validate()

        and : "It should form correct url"


        String expectedSearchUrl = "https://wwwdev.ebi.ac.uk/ebisearch/ws/rest/biomodels_parameters?" +
            "fields=entity_RAW,entity_id,initial_data_RAW,reaction_RAW,reaction_original_RAW,model,organism,publication," +
                "rate_RAW,rate_original_RAW,parameters_RAW,entity_accession_url,reaction_sbo_term_link,entity_sbo_term_link,external_links" +
            "&query=GO%5C%3A0005892&size=10&start=0&sort=entity:ascending&format=json"
        String actualSearchUrl = command.getSearchUrl("json")
        expectedSearchUrl == actualSearchUrl
    }

    void "test ParameterSearchCommand for default options"() {

        given: "A parameter search command object is defined with negative criteria"
        def bindingMap = [query: "E4P*", size: null, start: null, sort: "model:ascending"]
        ParameterSearchCommand command = new ParameterSearchCommand(bindingMap)

        when: "When command is validated with positive criteria"
        then: "Validation should return true"
        command.validate()

        and : "It should return default command object"

        command.size == 10
        command.start == 0

    }

    void "test ParameterSearchCommand for Cross side scripting"() {

        given: "A parameter search command object is defined with negative criteria"
        def bindingMap = [query: "<script>alert('hi')</script>", size: null, start: null, sort: "<script>alert('hi')</script>"]
        ParameterSearchCommand command = new ParameterSearchCommand(bindingMap)

        when: "When command is validated with positive criteria"
        then: "Validation should return true"
        command.validate()

        and : "It should return default command object"

        command.query == "&lt;script&gt;alert(&#39;hi&#39;)&lt;/script&gt;"
        command.sort == "&lt;script&gt;alert(&#39;hi&#39;)&lt;/script&gt;"

    }
}
