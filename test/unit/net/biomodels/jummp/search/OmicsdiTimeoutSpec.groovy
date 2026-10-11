package net.biomodels.jummp.search

import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import spock.lang.Specification

class OmicsdiTimeoutSpec extends Specification {

    private static int timeoutOf(SimpleClientHttpRequestFactory f, String field) {
        def reflected = SimpleClientHttpRequestFactory.getDeclaredField(field)
        reflected.accessible = true
        reflected.getInt(f)
    }

    void "the defaults apply when nothing is configured"() {
        given:
        def factory = new SimpleClientHttpRequestFactory()
        def template = new RestTemplate(factory)

        when:
        OmicsdiBasedSearch.applyTimeouts(template, null)

        then:
        timeoutOf(factory, "connectTimeout") == OmicsdiBasedSearch.DEFAULT_CONNECT_TIMEOUT_MS
        timeoutOf(factory, "readTimeout") == OmicsdiBasedSearch.DEFAULT_READ_TIMEOUT_MS
    }

    void "the configured values override the defaults"() {
        given:
        def factory = new SimpleClientHttpRequestFactory()
        def template = new RestTemplate(factory)
        def cfg = [connectTimeoutMs: 1234, readTimeoutMs: "4321"]

        when:
        OmicsdiBasedSearch.applyTimeouts(template, cfg)

        then:
        timeoutOf(factory, "connectTimeout") == 1234
        timeoutOf(factory, "readTimeout") == 4321
    }

    void "a request factory that is not the simple one is left alone"() {
        given:
        def template = Mock(RestTemplate)
        template.getRequestFactory() >> null

        when:
        OmicsdiBasedSearch.applyTimeouts(template, null)

        then:
        noExceptionThrown()
    }
}
