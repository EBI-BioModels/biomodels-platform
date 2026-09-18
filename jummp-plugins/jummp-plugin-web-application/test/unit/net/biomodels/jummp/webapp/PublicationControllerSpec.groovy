package net.biomodels.jummp.webapp

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import net.biomodels.jummp.core.IPublicationService
import net.biomodels.jummp.core.constants.BioModels
import net.biomodels.jummp.core.model.PublicationTransportCommand as PTC
import net.biomodels.jummp.core.user.PersonTransportCommand
import net.biomodels.jummp.model.Publication
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@Mock(Publication)
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
            authors: authors, link: "${BioModels.BM_ROOT_URL}/dev")
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

    // JBM-711: edit/save/fetchPublicationFromPubMedAndRenderPublicationForm must be reachable by
    // curators/admins OR by a user who owns (has write access to) a model the publication belongs
    // to -- and must forward to errors/error403 for everyone else.

    void "edit forwards to error403 when the user cannot manage the publication"() {
        given:
        Publication publication = new Publication(title: "Someone else's publication").save(validate: false)
        def publicationService = mockFor(IPublicationService)
        publicationService.demand.canManagePublication(1) { Publication p -> false }
        controller.publicationService = publicationService.createMock()

        when:
        controller.edit(publication)

        then:
        response.forwardedUrl == "/errors/error403"
    }

    void "edit renders the edit view when the user can manage the publication"() {
        given:
        Publication publication = new Publication(title: "My own publication").save(validate: false)
        def publicationService = mockFor(IPublicationService)
        publicationService.demand.canManagePublication(1) { Publication p -> true }
        controller.publicationService = publicationService.createMock()

        when:
        def result = controller.edit(publication)

        then:
        result.publication.id == publication.id
        response.forwardedUrl == null
    }

    void "edit renders a 404 for a missing publication without checking manage permission"() {
        given:
        def publicationService = mockFor(IPublicationService)
        controller.publicationService = publicationService.createMock()

        when:
        controller.edit(null)

        then:
        view == "/publication/error404"
        response.forwardedUrl == null
    }

    void "save forwards to error403 for a new publication when the user is not curation staff"() {
        given:
        def publicationService = mockFor(IPublicationService)
        publicationService.demand.isCurationStaff(1) { -> false }
        controller.publicationService = publicationService.createMock()
        PTC pubCmd = new PTC(id: -1, title: "A brand new publication")

        when:
        controller.save(pubCmd)

        then:
        response.forwardedUrl == "/errors/error403"
    }

    void "save proceeds past the permission gate for a new publication when the user is curation staff"() {
        given: "a permission check that succeeds -- whatever validate()/fromCommandObject then do is out of scope here"
        def publicationService = mockFor(IPublicationService)
        publicationService.demand.isCurationStaff(1) { -> true }
        controller.publicationService = publicationService.createMock()
        PTC pubCmd = new PTC(id: -1, title: "A brand new publication")

        when:
        controller.save(pubCmd)

        then: "no error403 forward happened, i.e. the permission gate let it through"
        response.forwardedUrl == null
    }

    void "save forwards to error403 for an existing publication the user cannot manage"() {
        given:
        Publication existing = new Publication(title: "Someone else's publication").save(validate: false)
        def publicationService = mockFor(IPublicationService)
        publicationService.demand.canManagePublication(1) { Publication p -> false }
        controller.publicationService = publicationService.createMock()
        PTC pubCmd = new PTC(id: existing.id, title: "Trying to overwrite it")

        when:
        controller.save(pubCmd)

        then:
        response.forwardedUrl == "/errors/error403"
    }

    void "save proceeds past the permission gate for an existing publication the user can manage"() {
        given: "a permission check that succeeds -- whatever validate()/fromCommandObject then do is out of scope here"
        Publication existing = new Publication(title: "My own publication").save(validate: false)
        def publicationService = mockFor(IPublicationService)
        publicationService.demand.canManagePublication(1) { Publication p -> true }
        controller.publicationService = publicationService.createMock()
        PTC pubCmd = new PTC(id: existing.id, title: "An update I am allowed to make")

        when:
        controller.save(pubCmd)

        then: "no error403 forward happened, i.e. the permission gate let it through"
        response.forwardedUrl == null
    }

    void "fetchPublicationFromPubMedAndRenderPublicationForm forwards to error403 editing a publication the user cannot manage"() {
        given:
        Publication existing = new Publication(title: "Someone else's publication").save(validate: false)
        def publicationService = mockFor(IPublicationService)
        publicationService.demand.canManagePublication(1) { Publication p -> false }
        controller.publicationService = publicationService.createMock()
        controller.params.operation = "edit"
        controller.params.id = existing.id as String

        when:
        controller.fetchPublicationFromPubMedAndRenderPublicationForm()

        then:
        response.forwardedUrl == "/errors/error403"
    }

    void "fetchPublicationFromPubMedAndRenderPublicationForm forwards to error403 adding a new publication when the user is not curation staff"() {
        given:
        def publicationService = mockFor(IPublicationService)
        publicationService.demand.isCurationStaff(1) { -> false }
        controller.publicationService = publicationService.createMock()
        controller.params.operation = "add"

        when:
        controller.fetchPublicationFromPubMedAndRenderPublicationForm()

        then:
        response.forwardedUrl == "/errors/error403"
    }
}
