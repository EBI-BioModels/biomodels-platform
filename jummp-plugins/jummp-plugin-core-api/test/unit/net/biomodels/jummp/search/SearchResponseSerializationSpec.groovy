package net.biomodels.jummp.search

import net.biomodels.jummp.core.model.ModelFormatTransportCommand
import net.biomodels.jummp.core.model.ModelState
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.PublicationLinkProviderTransportCommand
import net.biomodels.jummp.core.model.PublicationTransportCommand
import net.biomodels.jummp.core.user.PersonTransportCommand
import net.biomodels.jummp.qcinfo.FlagLevel
import spock.lang.Specification
import uk.ac.ebi.ddi.ebe.ws.dao.model.common.Facet
import uk.ac.ebi.ddi.ebe.ws.dao.model.common.FacetValue

/**
 * The searchResults cache overflows to disk, which Java-serializes the cached SearchResponse (JBM-804).
 */
class SearchResponseSerializationSpec extends Specification {
    private static <T> T roundTrip(T original) {
        def bytes = new ByteArrayOutputStream()
        new ObjectOutputStream(bytes).withCloseable { it.writeObject(original) }
        // the test runner's context classloader is not the one that loaded our classes
        def input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())) {
            @Override
            protected Class<?> resolveClass(ObjectStreamClass desc) {
                Class.forName(desc.name, false, SearchResponse.classLoader)
            }
        }
        return input.withCloseable { it.readObject() } as T
    }

    private static Facet facet() {
        FacetValue v1 = new FacetValue(label: "Homo sapiens", value: "9606", count: "12")
        FacetValue v2 = new FacetValue(label: "Mus musculus", value: "10090", count: "3")
        new Facet(id: "species", label: "Organism", total: 2, facetValues: [v1, null, v2] as FacetValue[])
    }

    private static ModelTransportCommand model() {
        new ModelTransportCommand(id: 1L, submissionId: "MODEL2401190005", publicationId: "BIOMD0000000001",
                name: "Edelstein1996", state: ModelState.PUBLISHED, flagLevel: FlagLevel.FLAG_1,
                format: new ModelFormatTransportCommand(id: 2L, identifier: "SBML", name: "SBML"),
                creators: ["A. Author"] as Set, contributors: [tung: "Tung"], submissionDate: new Date(),
                publication: new PublicationTransportCommand(id: 3L, title: "A paper", year: 1996,
                        linkProvider: new PublicationLinkProviderTransportCommand(),
                        authors: [new PersonTransportCommand(userRealName: "A. Author")]))
    }

    void "a populated SearchResponse survives a Java serialization round trip"() {
        given:
        SearchResponse original = new SearchResponse(totalCount: 42L,
                results: [model()], facets: [species: new OrderedFacet(facet(), 2)])

        when:
        SearchResponse copy = roundTrip(original)

        then:
        copy.totalCount == 42L
        copy.results*.submissionId == ["MODEL2401190005"]
        copy.results[0].state == ModelState.PUBLISHED
        copy.results[0].publication.authors*.userRealName == ["A. Author"]
        copy.facets.keySet() as List == ["species"]
        copy.facets.species.order == 2
    }

    void "OrderedFacet keeps every field of the wrapped Facet"() {
        when:
        OrderedFacet copy = roundTrip(new OrderedFacet(facet(), 5))

        then:
        copy.order == 5
        copy.facet.id == "species"
        copy.facet.label == "Organism"
        copy.facet.total == 2
        copy.facet.facetValues.length == 3
        copy.facet.facetValues[0].label == "Homo sapiens"
        copy.facet.facetValues[0].value == "9606"
        copy.facet.facetValues[0].count == "12"
        copy.facet.facetValues[1] == null
        copy.facet.facetValues[2].value == "10090"
    }

    void "OrderedFacet copes with a missing Facet and missing facet values"() {
        expect:
        roundTrip(new OrderedFacet(null, 1)).facet == null
        roundTrip(new OrderedFacet(new Facet(id: "empty"), 1)).facet.facetValues == null
    }
}
