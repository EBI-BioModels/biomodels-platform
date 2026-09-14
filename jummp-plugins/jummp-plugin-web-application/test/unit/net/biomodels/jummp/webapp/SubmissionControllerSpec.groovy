package net.biomodels.jummp.webapp

import grails.test.mixin.TestFor
import net.biomodels.jummp.core.model.ModelTransportCommand as MTC
import net.biomodels.jummp.core.model.RevisionTransportCommand as RTC
import spock.lang.Specification
import spock.lang.Unroll

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(SubmissionController)
class SubmissionControllerSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    // JBM-770: on an update/amend, the RTC handed to populateDataRevision() is the latest
    // revision's own transport command, so its .comment already holds that (old) revision's
    // commit message before this method ever runs. Leaving the "Explain what you have updated"
    // box empty must still fall back to the default message, not silently keep whatever comment
    // the RTC happened to inherit.
    @Unroll
    void "populateDataRevision falls back to the default message when left blank, regardless of an inherited comment (isUpdate=#isUpdate)"() {
        given:
        MTC model = new MTC(name: "Some model", description: "Some description")
        RTC revision = new RTC(comment: "added euro xml") // simulates the latest revision's carried-over RTC

        when:
        controller.populateDataRevision(revision, model, [:], [], "", isUpdate)

        then:
        revision.comment == "Model revised without commit message"

        where:
        isUpdate << [true, false]
    }

    void "populateDataRevision uses the submitter's own comment when one is provided, even over an inherited one"() {
        given:
        MTC model = new MTC(name: "Some model", description: "Some description")
        RTC revision = new RTC(comment: "added euro xml")

        when:
        controller.populateDataRevision(revision, model, [:], [], "fixed the typo in the description", true)

        then:
        revision.comment == "fixed the typo in the description"
    }
}
