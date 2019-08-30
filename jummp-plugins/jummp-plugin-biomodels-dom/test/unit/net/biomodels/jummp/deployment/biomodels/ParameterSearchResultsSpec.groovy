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

        ParameterSearchResults results = getResultObjectWithQuery("E4P*")

        then: "It should return correct results"

        results.recordsTotal > 1

        and: "it should return reaction value"


        String expectedPublication = "http://identifiers.org/pubmed/22001849|22001849"
        String expectedReaction = "([24794350] + [122357]) => ([sedoheptulose 1,7-bisphosphate])"
        String expectedOriginalReaction = "(TP + E4P) => (SBP)"
        String expectedReactionShow = "<span class='legend-green'>(TP + E4P) => (SBP)</span><br/><br/>([24794350] + [122357]) => ([sedoheptulose 1,7-bisphosphate])"
        String expectedEntityId = "E4P"
        String expectedEntityShow = "<span class='legend-green'>E4P</span><br/><br/><a style='color:black' target='_blank' href='http://identifiers.org/pubchem.compound/122357' > 122357 </a>"
        String expectedRateShow = "<span class='legend-green'>chloroplast*Vm*(DHAP*E4P-SBP/q)/((DHAP+Ks1)*(E4P+Ks2))</span><br/><br/>chloroplast*Vm*([668]*[122357]-[sedoheptulose 1,7-bisphosphate]/q)/(([668]+Ks1)*([122357]+Ks2))"
        String expectedParameters = "q=1.017; Vm=1.21889; Ks2=0.2; Ks1=0.4"
        results.entries.find{value ->
           value.fields.entity_id == expectedEntityId &&
               value.fields.reaction == expectedReaction &&
               value.fields.reaction_original_RAW == expectedOriginalReaction &&
               value.fields.publication == expectedPublication &&
               value.fields.reaction_show == expectedReactionShow &&
               value.fields.entity_show == expectedEntityShow &&
               value.fields.rate_show == expectedRateShow &&
               value.fields.parameters == expectedParameters
        }

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


    private static ParameterSearchResults getResultObjectWithQuery(String query) {
        String urlString = "https://wwwdev.ebi.ac.uk/ebisearch/ws/rest/biomodels_parameters?format=json" +
            "&fields=entity_RAW,entity_id,reaction_RAW,entity_accession_url,reaction_original_RAW,model,publication," +
            "rate_RAW,rate_original_RAW,parameters_RAW,external_links,initial_data_RAW&query=${query}&size=10&start=0&sort=entity:ascending"
        def searchResults = urlString.toURL().text
        searchResults = ParameterSearchService.replaceFieldNames(searchResults)
        ParameterSearchResults results = ParameterSearchResults.fromJson(JSON.parse(searchResults))
        results
    }


}
