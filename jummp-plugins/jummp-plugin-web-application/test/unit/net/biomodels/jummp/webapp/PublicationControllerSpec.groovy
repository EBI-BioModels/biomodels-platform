package net.biomodels.jummp.webapp

import grails.test.mixin.TestFor
import net.biomodels.jummp.core.IPublicationService
import net.biomodels.jummp.core.model.PublicationTransportCommand as PTC
import net.biomodels.jummp.core.user.PersonTransportCommand
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(PublicationController)
class PublicationControllerSpec extends Specification {

    List<PTC> allPublications = new ArrayList<>()

    def setup() {
        PersonTransportCommand author = new PersonTransportCommand(id: 1, userRealName: "Tung Nguyen")
        List authors = [author]
        PTC ptc = new PTC(id: 1,
            title: "How to test Grails controller in an effectively way",
            journal: "BioModels\' developers review", affiliation: "EBI BioModels",
            synopsis: "Spock is among testing framework favourites we could consider to make use in our projects",
            authors: authors, link: "https://www.ebi.ac.uk/biomodels/dev")
        allPublications.add(ptc)
    }

    void "test index action"() {
        given:
        1 == allPublications.size()
        def publicationService = mockFor(IPublicationService)
        publicationService.demand.all { ->
            allPublications
        }
        controller.publicationService = publicationService.createMock()
        /*IPublicationService publicationService = Mock()
        controller.publicationService = publicationService*/
        when:
        def result = controller.index()
        then:
        1 == result.publications.size()
        "List of all publications" == result.title
    }
}
