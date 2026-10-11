package net.biomodels.jummp.search

import net.biomodels.jummp.core.model.ModelFormatTransportCommand
import net.biomodels.jummp.core.model.ModelState
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.PublicationTransportCommand
import spock.lang.Specification
import uk.ac.ebi.ddi.ebe.ws.dao.model.common.Facet
import uk.ac.ebi.ddi.ebe.ws.dao.model.common.FacetValue

class SearchResultCodecSpec extends Specification {

    void "published and unpublished models survive the round trip"() {
        given:
        def published = new ModelTransportCommand(submissionId: "MODEL1", publicationId: "BIOMD1",
                name: "Glycolysis", description: "d", submitter: "Jane", submitterUsername: "jane",
                submissionDate: new Date(1000L), lastModifiedDate: new Date(2000L),
                state: ModelState.PUBLISHED, searchableLink: "https://x/y",
                format: new ModelFormatTransportCommand(name: "SBML", formatVersion: "L3V1"),
                publication: new PublicationTransportCommand(year: 2020))
        def unpublished = new ModelTransportCommand(submissionId: "MODEL2", name: "Private",
                state: ModelState.UNPUBLISHED)

        when:
        List<ModelTransportCommand> decoded = SearchResultCodec.decodeModels(
                SearchResultCodec.encodeModels([published, unpublished]))

        then:
        decoded.size() == 2
        decoded[0].submissionId == "MODEL1"
        decoded[0].publicationId == "BIOMD1"
        decoded[0].name == "Glycolysis"
        decoded[0].submissionDate == new Date(1000L)
        decoded[0].lastModifiedDate == new Date(2000L)
        decoded[0].state == ModelState.PUBLISHED
        decoded[0].format.name == "SBML"
        decoded[0].format.formatVersion == "L3V1"
        decoded[0].publication.year == 2020
        decoded[0].searchableLink == "https://x/y"
        decoded[1].submissionId == "MODEL2"
        decoded[1].state == ModelState.UNPUBLISHED
        decoded[1].format == null
        decoded[1].submissionDate == null
    }

    void "facets and their values survive the round trip"() {
        given:
        def fv = new FacetValue(label: "SBML", value: "SBML", count: "42")
        def facet = new Facet(id: "modelformat", label: "Model format", total: 1)
        facet.facetValues = [fv] as FacetValue[]

        when:
        List<Facet> decoded = SearchResultCodec.decodeFacets(SearchResultCodec.encodeFacets([facet]))

        then:
        decoded.size() == 1
        decoded[0].id == "modelformat"
        decoded[0].label == "Model format"
        decoded[0].total == 1
        decoded[0].facetValues.length == 1
        decoded[0].facetValues[0].value == "SBML"
        decoded[0].facetValues[0].count == "42"
    }

    void "entries in the old id|name format are rejected so that they count as a cache miss"() {
        when:
        SearchResultCodec.decodeModels("1|Glycolysis2|Other")

        then:
        thrown(Exception)
    }

    void "a JSON value that is not an array is rejected"() {
        when:
        SearchResultCodec.decodeFacets('{"id": "x"}')

        then:
        thrown(IllegalArgumentException)
    }
}
