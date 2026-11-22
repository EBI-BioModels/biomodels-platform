package net.biomodels.jummp.search

import spock.lang.Specification
import spock.lang.Unroll

class EbiSearchServerSpec extends Specification {

    @Unroll
    void "no usable setting (#setting.class.simpleName) gives www.ebi.ac.uk"() {
        when:
        def config = EbiSearchServer.configFor(setting)

        then:
        config.protocol == "https"
        config.hostName == "www.ebi.ac.uk"

        where:
        setting << [null, "", "   ", new ConfigObject()]
    }

    @Unroll
    void "the server '#setting' is used as #protocol://#host"() {
        when:
        def config = EbiSearchServer.configFor(setting)

        then:
        config.protocol == protocol
        config.hostName == host

        where:
        setting                          | protocol | host
        "https://www.ebi.ac.uk"          | "https"  | "www.ebi.ac.uk"
        "https://www.ebi.ac.uk/"         | "https"  | "www.ebi.ac.uk"
        " http://localhost:8000 "        | "http"   | "localhost:8000"
        "https://search.example.org"     | "https"  | "search.example.org"
    }

    @Unroll
    void "'#setting' is refused"() {
        when:
        EbiSearchServer.configFor(setting)

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains("jummp.search.omicsdi.baseUrl")

        where:
        setting << ["www.ebi.ac.uk", "ftp://www.ebi.ac.uk", "https://www.ebi.ac.uk/ebisearch",
                    "https://www.ebi.ac.uk?x=1", "https://", "https://bad host"]
    }
}
