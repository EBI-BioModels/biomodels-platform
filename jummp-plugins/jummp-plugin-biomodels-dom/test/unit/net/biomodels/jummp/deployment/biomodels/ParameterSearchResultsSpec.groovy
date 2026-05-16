package net.biomodels.jummp.deployment.biomodels

import grails.converters.JSON
import grails.test.mixin.TestMixin
import grails.test.mixin.services.ServiceUnitTestMixin
import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchResults
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestMixin(ServiceUnitTestMixin)

class ParameterSearchResultsSpec extends Specification {


    void "test ParameterSearchResults fromJSON"() {

        given: "A webservice call to ebi search and basic criteria"

        when: "Called getResultObjectWithQuery to hit the webservice and get the results"
        ParameterSearchResults results = getResultObjectWithQuery("E4P*", "model:ascending")

        then: "It should return correct results"
        results.recordsTotal > 1

        and: "it should return reaction value"
        String expectedPublication = "http://identifiers.org/pubmed/15233787|15233787"
        String expectedReaction = "GraP + Sed7P => E4P + Fru6P"
        String expectedOriginalReaction = "GraP + Sed7P => E4P + Fru6P"
        String expectedReactionShow = "K6v24=0.4653 dimensionless; Keqv24=1.05 dimensionless; K5v24=0.8683 dimensionless; Vmaxv24=27.2 mM_per_hour; K1v24=0.00823 mM; K2v24=0.04765 mM; K7v24=2.524 dimensionless; K3v24=0.1733 mM; K4v24=0.006095 mM"
        String expectedEntityId = "E4P"
        String expectedEntityShow = "<span class='legend-green'>E4P</span><br/><br/><a style='color:black' target='_blank' href='http://identifiers.org/chebi/CHEBI:16897' > D-erythrose 4-phosphate(2-) </a>; <a style='color:black' target='_blank' href=' http://identifiers.org/kegg.compound/C00279' > D-Erythrose 4-phosphate </a>"
        String expectedParameters = "K6v24=0.4653 dimensionless; Keqv24=1.05 dimensionless; K5v24=0.8683 dimensionless; Vmaxv24=27.2 mM_per_hour; K1v24=0.00823 mM; K2v24=0.04765 mM; K7v24=2.524 dimensionless; K3v24=0.1733 mM; K4v24=0.006095 mM"

        // The results likely have the entity identified E4P
        results.entries.findAll() {value ->
           value.fields.entity_id == expectedEntityId &&
               value.fields.reaction == expectedReaction &&
               value.fields.reaction_original_RAW == expectedOriginalReaction &&
               value.fields.publication == expectedPublication &&
               value.fields.entity_show == expectedEntityShow &&
               value.fields.parameters == expectedParameters
        }.size() > 0
    }

    void "test ParameterSearchResults fromJSON for html escaping special character"() {

        given: "A webservice call to ebi search and basic criteria"
        when: "Called getResultObjectWithQuery to hit the webservice and get the results"
        ParameterSearchResults results = getResultObjectWithQuery("BIO*122")

        then: "it should return correct initial data value with special character units"

        String expectedInitialData = "9.477E-4 μmol"

        results.entries.find{value ->
            value.fields.initial_data == expectedInitialData
        }

    }

    void "test ParameterSearchResults fromJSON for External links"() {

        given: "A webservice call to ebi search and basic criteria"
        when: "Called getResultObjectWithQuery to hit the webservice and get the results"
        ParameterSearchResults results = ParameterSearchResults.fromJson(JSON.parse(
            '''
             {"hitCount" : 1,
                "entries":[{"fields":{"entity":"e1",
                "external_links" : "reactome:1234;sabiork.compound:9877" }}]
                }'''))

        then: "it should return correct external links value"
        String expectedExternalLinksShow = '''<a href="https://reactome.org/content/query?q=1234" target="_blank">reactome:1234</a>;\
 <a href="http://sabiork.h-its.org/newSearch?q=9877" target="_blank">sabiork.compound:9877</a>'''

        results.entries.find{value ->
           value.fields.external_links_show == expectedExternalLinksShow
        }

    }

    void "test the external links containing OpenTargets links"() {
        given: "A webservice call to ebi search and basic criteria"
        when: "Called getResultObjectWithQuery to hit the webservice and get the results"
        ParameterSearchResults results = getResultObjectWithQuery("BIO*700")

        then: "it should return correct initial data value with special character units"

        String expected = "OpenTargets:"
        def externalLinks = results.entries.collect() {
            it.fields["external_links_show"]
        }
        externalLinks.size() > 0
        externalLinks.find {
            it.contains(expected)
        }.size() > 0
    }

    private static ParameterSearchResults getResultObjectWithQuery(String query,
                                                                   String sortField = "entity:ascending") {
        String urlString = "https://wwwdev.ebi.ac.uk/ebisearch/ws/rest/biomodels_parameters?format=json" +
            "&fields=entity_RAW,entity_id,reaction_RAW,entity_accession_url,reaction_original_RAW,model,publication," +
            "rate_RAW,rate_original_RAW,parameters_RAW,external_links,external_links_show," +
            "initial_data_RAW&query=${query}&size=10&start=0&sort=${sortField}"
        def searchResults = urlString.toURL().text
        searchResults = ParameterSearchService.replaceFieldNames(searchResults)
        ParameterSearchResults results = ParameterSearchResults.fromJson(JSON.parse(searchResults))
        results
    }


}
