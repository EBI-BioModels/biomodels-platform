package net.biomodels.jummp.webapp

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * Covers JBM-801: renderPublicationDetails shows the publication in the summary step of the submission wizard. When the
 * page has no publication it sends "", {} or null, and it must say so and not fail (a server error, which the page only
 * logs, left the summary without an explanation).
 *
 * The specs of the plugins' test/unit directories are not run from the root of the project, so this one is here, where
 * it runs.
 */
@TestFor(PublicationController)
class PublicationControllerRenderSpec extends Specification {
    void setup() {
        // the parameters of the summary step are html-encoded
        mockCodec(org.codehaus.groovy.grails.plugins.codecs.HTMLCodec)
    }

    void "the summary says that there is no publication when the page sends #description"() {
        given:
        controller.publicationService = [assembleAuthors: { def tc, def authors -> throw new AssertionError("Not asked") }]
        if (pubDetails != null) {
            params.pubDetails = pubDetails
        }

        when:
        controller.renderPublicationDetails()

        then:
        response.status == 200
        response.text == "No publication provided"

        where:
        description                | pubDetails
        "an empty string"          | '""'
        "an empty object"          | "{}"
        "null"                     | "null"
        "nothing at all"           | null
    }
}
