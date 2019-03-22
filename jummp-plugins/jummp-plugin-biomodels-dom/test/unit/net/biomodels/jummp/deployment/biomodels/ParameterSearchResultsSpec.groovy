package net.biomodels.jummp.deployment.biomodels

import grails.converters.JSON
import grails.test.mixin.TestFor
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

        String urlString = "https://wwwdev.ebi.ac.uk/ebisearch/ws/rest/biomodels_parameters?format=json" +
            "&fields=entity_RAW,entity_id,reaction_RAW,entity_accession_url,reaction_original_RAW,model,publication,rate_RAW,rate_original_RAW,parameters_RAW" +

            "&query=E4P*&size=10&start=0&sort=entity:ascending"
        def searchResults = urlString.toURL().text
        searchResults = ParameterSearchService.replaceFieldNames(searchResults)
        when: "Converted parsed from JSON with ParameterSearchResults"
        ParameterSearchResults results = ParameterSearchResults.fromJson(JSON.parse(searchResults))

        then: "It should return correct results"

        results.recordsTotal > 1

        and: "it should return reaction value"


        String expectedPublication = "http://identifiers.org/pubmed/22001849|22001849"
        String expectedReaction = "([24794350] + [122357]) => ([sedoheptulose 1,7-bisphosphate])"
        String expectedOriginalReaction = "(TP + E4P) => (SBP)"
        String expectedReactionShow = "([24794350] + [122357]) => ([sedoheptulose 1,7-bisphosphate])<hr/><span class='legend-green'>(TP + E4P) => (SBP)</span>"
        String expectedEntityId = "E4P"
        String expectedEntityShow = "<a target='_blank' href='http://identifiers.org/pubchem.compound/122357' > 122357 </a><hr/><span class='legend-green'>E4P</span>"
        String expectedRateShow = "chloroplast*function_7(Vm, [668], [122357], [sedoheptulose 1,7-bisphosphate], q, Ks1, Ks2)<hr/><span class='legend-green'>chloroplast*function_7(Vm, DHAP, E4P, SBP, q, Ks1, Ks2)</span>"


        results.entries.find{value ->
           value.fields.entity_id == expectedEntityId &&
               value.fields.reaction == expectedReaction &&
               value.fields.reaction_original_RAW == expectedOriginalReaction &&
               value.fields.publication == expectedPublication &&
               value.fields.reaction_show == expectedReactionShow &&
               value.fields.entity_show == expectedEntityShow &&
               value.fields.rate_show == expectedRateShow
        }

    }


}
