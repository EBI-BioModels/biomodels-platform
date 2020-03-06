package net.biomodels.jummp.deployment.biomodels

import grails.converters.JSON
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchCommand
import spock.lang.Specification

@TestMixin(GrailsUnitTestMixin)
class ParameterSearchCommandSpec extends Specification {
    private static ParameterSearchCommand prepareCommandObject(Map bindingMap) {
        return new ParameterSearchCommand(bindingMap)
    }
    static final String expectedUrlPrefix = "https://wwwdev.ebi.ac.uk/ebisearch/ws/rest/biomodels_parameters?fields=${ParameterSearchCommand.EBI_SEARCH_FIELDS_CSV}"
    static final String expectedUrlSuffix = "&size=10&start=0&sort=&format=json"

    void "test ParameterSearchCommand Positively"() {
        when: "A parameter search command object is defined with basic criteria"
        ParameterSearchCommand command = prepareCommandObject([query: "E4P*", size: 10, start: 0, sort: "entity:ascending"])

        then: "The command is valid"
        command.validate()
    }

    void "test ParameterSearchCommand Negatively with size and start"() {
        when: "A parameter search command object is defined with negative criteria"
        ParameterSearchCommand command = prepareCommandObject([query: "E4P*", size: 13, start: -5, sort: "entity:ascending"])

        then: "Validation should return false"
        !command.validate()
    }

    void "test ParameterSearchCommand positively with query = *"() {
        when: "A parameter search command object without a query is created"
        ParameterSearchCommand command = prepareCommandObject([query: null, size: 10, start: 0, sort: "entity:ascending"])

        then: "it is valid, and we fall back on the default query -- *:*"
        command.validate()
        assertEquals("*:*", command.query)
    }

    void "test ParameterSearchCommand URL"() {
        when: "a parameter search command object for 'E4*' is created"
        ParameterSearchCommand command = prepareCommandObject([query: "E4P*", size: 10, start: 0, sort: "entity:ascending"])

        then: "the command is valid"
        command.validate()

        and: "it should form correct url"
        String expectedSearchUrl = "https://wwwdev.ebi.ac.uk/ebisearch/ws/rest/biomodels_parameters?fields=entity_RAW," +
            "entity_id,initial_data_RAW,reaction_RAW,reaction_original_RAW,model,organism,publication,rate_RAW,rate_original_RAW," +
            "parameters_RAW,entity_accession_url,reaction_sbo_term_link,entity_sbo_term_link,external_links&query=E4P*+AND+is_curated%3Atrue&" +
            "size=10&start=0&sort=entity:ascending&format=json"
        String actualSearchUrl = command.getSearchUrl("json")
        expectedSearchUrl == actualSearchUrl
    }

    void "should compute the search url"(String query, String link) {
        when:
        ParameterSearchCommand cmd = new ParameterSearchCommand(query: query)

        then:
        cmd.validate()
        URL url = new URL(link)
        cmd.getSearchUrl("json") == url

        when:
        String text = url.text

        then:
        noExceptionThrown()

        when:
        def response = JSON.parse(text)
        then:
        response.hitCount > 0
        response.entries.length() == Math.min(((Integer) response.hitCount).intValue(), cmd.size.intValue())

        where:
        query               | link
        "model:BIOMD*292"   | "${expectedUrlPrefix}&query=model%3ABIOMD*292+AND+is_curated%3Atrue${expectedUrlSuffix}"
        "model%3ABIOMD*292" | "${expectedUrlPrefix}&query=model%3ABIOMD*292+AND+is_curated%3Atrue${expectedUrlSuffix}"
        "BIOMD*292"         | "${expectedUrlPrefix}&query=BIOMD*292+AND+is_curated%3Atrue${expectedUrlSuffix}"
        "entity_accession_url:CHEBI:16474 OR CHEBI:15355 OR publication:8983160" | "${expectedUrlPrefix}&query=entity_accession_url%3ACHEBI%5C%3A16474+OR+CHEBI%5C%3A15355+OR+publication%3A8983160+AND+is_curated%3Atrue${expectedUrlSuffix}"
    }

    void "test ParameterSearchCommand URL with special character for accession"() {
        when: "A ParameterSearchCommand is created for a query containing a ':' character"
        ParameterSearchCommand command = prepareCommandObject([query: "GO:0005892", size: 10, start: 0, sort: "entity:ascending"])

        then: "The command is valid"
        command.validate()

        and: "It should form correct url"
        String expectedSearchUrl = "https://wwwdev.ebi.ac.uk/ebisearch/ws/rest/biomodels_parameters?" +
            "fields=entity_RAW,entity_id,initial_data_RAW,reaction_RAW,reaction_original_RAW,model,organism,publication," +
                "rate_RAW,rate_original_RAW,parameters_RAW,entity_accession_url,reaction_sbo_term_link,entity_sbo_term_link,external_links" +
            "&query=GO%5C%3A0005892+AND+is_curated%3Atrue&size=10&start=0&sort=entity:ascending&format=json"
        String actualSearchUrl = command.getSearchUrl("json")
        expectedSearchUrl == actualSearchUrl
    }

    void "test ParameterSearchCommand for default options"() {
        when: "A parameter search command object is defined without specifying pagination criteria or curation status"
        ParameterSearchCommand command = prepareCommandObject([query: "E4P*", size: null, start: null, sort: "model:ascending", is_curated:null])

        then: "it is valid"
        command.validate()

        and : "the default field values should be used"
        command.size == 10
        command.start == 0
        command.is_curated
    }

    void "test ParameterSearchCommand for cross-site scripting"() {
        when: "A parameter search command object contains a malicious query"
        ParameterSearchCommand command = prepareCommandObject([query: "<script>alert('hi')</script>", size: null, start: null, sort: "<script>alert('hi')</script>"])

        then: "it passes validation"
        command.validate()

        and: "the query is HTML-encoded"
        command.query == "&lt;script&gt;alert(&#39;hi&#39;)&lt;/script&gt;"
        command.sort == "&lt;script&gt;alert(&#39;hi&#39;)&lt;/script&gt;"
    }
}
