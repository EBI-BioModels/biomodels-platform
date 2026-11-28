package net.biomodels.jummp

import spock.lang.Specification
import spock.lang.Unroll

class StaticAssetsUrlSpec extends Specification {

    @Unroll
    void "the configured URL '#configured' is used as '#expected'"() {
        expect:
        CommonController.resolveStaticAssetsURL(configured, "http://localhost:8080/biomodels") == expected

        where:
        configured                           | expected
        "https://assets.example.org/files"   | "https://assets.example.org/files"
        "https://assets.example.org/files//" | "https://assets.example.org/files"
        "  http://localhost:8000  "          | "http://localhost:8000"
    }

    @Unroll
    void "without a setting, the server #serverURL uses #expected"() {
        expect:
        CommonController.resolveStaticAssetsURL(configured, serverURL) == expected

        where:
        configured         | serverURL                         | expected
        null               | "http://localhost:8080/biomodels" | "https://www.biomodels.org/static-assets"
        new ConfigObject() | "http://localhost:8080/biomodels" | "https://www.biomodels.org/static-assets"
        "   "              | "https://wwwdev.ebi.ac.uk/biomodels" | "https://www.biomodels.org/static-assets"
        null               | "https://www.biomodels.org"       | "https://www.biomodels.org/static-assets"
        null               | "https://test.biomodels.net/"     | "https://test.biomodels.net/static-assets"
    }
}
