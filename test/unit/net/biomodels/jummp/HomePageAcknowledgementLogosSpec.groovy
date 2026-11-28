package net.biomodels.jummp

import grails.test.mixin.TestMixin
import grails.test.mixin.web.GroovyPageUnitTestMixin
import spock.lang.Specification
import spock.lang.Unroll

/** Renders the Acknowledgements card of the home page, which has no controller behind it. */
@TestMixin(GroovyPageUnitTestMixin)
class HomePageAcknowledgementLogosSpec extends Specification {
    static final List<String> LOGOS =
        ["EARLHAM_logo.png", "UFL_LSM_logo.png", "EMBL_logo_lo_res_0.jpg", "bbsrc_logo.png",
         "IMI_logo-small.png", "FP7_logo-small.png"]

    String savedUrl

    def setup() {
        // no controller has run yet
        savedUrl = CommonController.bmStaticAssetsURL
        CommonController.bmStaticAssetsURL = null
    }

    def cleanup() {
        CommonController.bmStaticAssetsURL = savedUrl
    }

    @Unroll
    void "the logos come from #expected when the server is #serverURL"() {
        given:
        grailsApplication.config.grails.serverURL = serverURL
        if (configured) {
            grailsApplication.config.jummp.web.staticAssets.server = configured
        }

        when:
        String html = render(template: "/templates/biomodels/homePage/hp-row-3")

        then:
        LOGOS.every { html.contains("src=\"$expected/images/$it\"") }
        !html.contains("wwwdev.ebi.ac.uk")

        where:
        serverURL                         | configured                            | expected
        "https://www.biomodels.org"       | null                                  | "https://www.biomodels.org/static-assets"
        "http://localhost:8080/biomodels" | null                                  | "https://www.biomodels.org/static-assets"
        "http://localhost:8080/biomodels" | "https://assets.example.org/assets/"  | "https://assets.example.org/assets"
    }
}
