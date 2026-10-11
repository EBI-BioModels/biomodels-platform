package net.biomodels.jummp.search

import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import spock.lang.Specification
import spock.lang.Unroll
import uk.ac.ebi.ddi.ebe.ws.dao.model.common.QueryResult

/**
 * How failures of a request to EBI Search are reported to OmicsdiBasedSearch.searchModels' callers.
 *
 * An exception is not kept by the cache on searchModels, while an empty result is (JBM-820).
 */
class OmicsdiErrorMappingSpec extends Specification {

    void "the result of a successful request is returned as it is"() {
        given:
        def expected = new QueryResult()

        expect:
        OmicsdiBasedSearch.executeSearchRequest("glucose") { expected }.is(expected)
    }

    void "a timeout is reported as an unavailable search server"() {
        given:
        def timeout = new ResourceAccessException("I/O error: Read timed out",
                new SocketTimeoutException("Read timed out"))

        when:
        OmicsdiBasedSearch.executeSearchRequest("glucose") { throw timeout }

        then:
        SearchUnavailableException e = thrown()
        e.cause.is(timeout)
    }

    void "a refused connection is reported as an unavailable search server"() {
        when:
        OmicsdiBasedSearch.executeSearchRequest("glucose") {
            throw new ResourceAccessException("I/O error: Connection refused",
                    new ConnectException("Connection refused"))
        }

        then:
        thrown(SearchUnavailableException)
    }

    void "an unknown search server is reported as an unavailable search server"() {
        given:
        def unknownHost = new UnknownHostException("www.ebi.ac.uk")

        when:
        OmicsdiBasedSearch.executeSearchRequest("glucose") { throw unknownHost }

        then:
        SearchUnavailableException e = thrown()
        e.cause.is(unknownHost)
    }

    @Unroll
    void "a #status answer is reported as an unavailable search server"() {
        when:
        OmicsdiBasedSearch.executeSearchRequest("glucose") { throw new HttpServerErrorException(status) }

        then:
        SearchUnavailableException e = thrown()
        e.message.contains(status.value().toString())

        where:
        status << [HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.BAD_GATEWAY,
                   HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.GATEWAY_TIMEOUT]
    }

    @Unroll
    void "a #status answer means the query was rejected, which is an empty result and not a failure"() {
        expect:
        OmicsdiBasedSearch.executeSearchRequest("glucose:[") {
            throw new HttpClientErrorException(status)
        } == null

        where:
        status << [HttpStatus.BAD_REQUEST, HttpStatus.NOT_FOUND]
    }

    void "other exceptions are not swallowed"() {
        when:
        OmicsdiBasedSearch.executeSearchRequest("glucose") { throw new IllegalStateException("boom") }

        then:
        thrown(IllegalStateException)
    }
}
