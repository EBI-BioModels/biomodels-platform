package net.biomodels.jummp.search

import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import spock.lang.Specification
import uk.ac.ebi.ddi.ebe.ws.dao.model.common.QueryResult

/**
 * EBI Search answers either at once or very late, so a search that timed out waiting for the answer is tried
 * once more (see OmicsdiBasedSearch.executeSearchRequest).
 */
class OmicsdiRetrySpec extends Specification {

    private static ResourceAccessException readTimeout() {
        new ResourceAccessException("I/O error: Read timed out", new SocketTimeoutException("Read timed out"))
    }

    void "a read timeout is followed by one more attempt, whose result is returned"() {
        given:
        def expected = new QueryResult()
        int retries = 0

        when:
        def result = OmicsdiBasedSearch.executeSearchRequest("glucose", { throw readTimeout() }, {
            retries++
            expected
        })

        then:
        result.is(expected)
        retries == 1
    }

    void "the first attempt's answer is used when it arrives, without a retry"() {
        given:
        def expected = new QueryResult()
        int retries = 0

        when:
        def result = OmicsdiBasedSearch.executeSearchRequest("glucose", { expected }, { retries++; null })

        then:
        result.is(expected)
        retries == 0
    }

    void "a second timeout is reported as an unavailable search server, after two attempts only"() {
        given:
        int attempts = 0

        when:
        OmicsdiBasedSearch.executeSearchRequest("glucose", { attempts++; throw readTimeout() },
                { attempts++; throw readTimeout() })

        then:
        SearchUnavailableException e = thrown()
        e.cause instanceof ResourceAccessException
        attempts == 2
    }

    void "a timeout is not retried when no retry is given"() {
        given:
        int attempts = 0

        when:
        OmicsdiBasedSearch.executeSearchRequest("glucose") { attempts++; throw readTimeout() }

        then:
        thrown(SearchUnavailableException)
        attempts == 1
    }

    void "a refused connection is not retried"() {
        given:
        int retries = 0

        when:
        OmicsdiBasedSearch.executeSearchRequest("glucose", {
            throw new ResourceAccessException("I/O error: Connection refused", new ConnectException("refused"))
        }, { retries++; null })

        then:
        thrown(SearchUnavailableException)
        retries == 0
    }

    void "an error answer from EBI Search is not retried"() {
        given:
        int retries = 0

        when:
        OmicsdiBasedSearch.executeSearchRequest("glucose", {
            throw new HttpServerErrorException(HttpStatus.BAD_GATEWAY)
        }, { retries++; null })

        then:
        thrown(SearchUnavailableException)
        retries == 0
    }

    void "a rejected query is not retried"() {
        given:
        int retries = 0

        when:
        def result = OmicsdiBasedSearch.executeSearchRequest("glucose:[", {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST)
        }, { retries++; new QueryResult() })

        then:
        result == null
        retries == 0
    }

    void "a rejected query on the second attempt is an empty result and not a failure"() {
        when:
        def result = OmicsdiBasedSearch.executeSearchRequest("glucose", { throw readTimeout() },
                { throw new HttpClientErrorException(HttpStatus.BAD_REQUEST) })

        then:
        result == null
    }

    void "the first attempt waits for 8 s unless configured otherwise, and the retry is off if it is not shorter"() {
        expect:
        OmicsdiBasedSearch.firstAttemptTimeoutMs(null) == 8000
        OmicsdiBasedSearch.firstAttemptTimeoutMs([firstAttemptTimeoutMs: "2500"]) == 2500
        OmicsdiBasedSearch.readTimeoutMs(null) == OmicsdiBasedSearch.DEFAULT_READ_TIMEOUT_MS
        OmicsdiBasedSearch.firstAttemptTimeoutMs(null) < OmicsdiBasedSearch.readTimeoutMs(null)
    }
}
