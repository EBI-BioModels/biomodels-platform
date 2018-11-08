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
            "&fields=entity_RAW,entity_id,reaction_RAW,model,publication,rate_RAW,parameters_RAW" +
            "&query=E4P*&size=10&start=0&sort=entity:ascending"
        def searchResults = urlString.toURL().text

        when: "Converted parsed from JSON with ParameterSearchResults"
        ParameterSearchResults results = ParameterSearchResults.fromJson(JSON.parse(searchResults))

        then: "It should return correct results"

        int expectedTotalRecords = 22
        int actualTotalRecords = results.recordsTotal
        assertEquals(expectedTotalRecords,actualTotalRecords)

        then: "it should return reaction value"

        String expectedReaction = "([24794350] + [122357]) => ([CHEBI:17969])"
        String actualReaction = results.entries.first().fields.reaction_RAW
        assertEquals(expectedReaction,actualReaction)

        String expectedEntityId = "E4P"
        String actualEntityId = results.entries.first().fields.entity_id
        assertEquals(expectedEntityId,actualEntityId)

    }


}
