package net.biomodels.jummp.search

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import net.biomodels.jummp.core.model.ModelState
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.utils.redis.RedisService
import net.biomodels.jummp.webapp.SearchController
import spock.lang.Specification

/**
 * Which requests for all models are answered from the Redis cache, and which go to EBI Search (JBM-820).
 */
@TestFor(SearchController)
@Mock([User])
class SearchControllerCacheSpec extends Specification {

    /** Records how the controller uses the search service. */
    static class StubSearchService {
        Map cached = [:]
        List<Map> searches = []
        List<Map> cacheWrites = []
        int cacheReads = 0
        Exception failure

        Map retrieveCachedSearchAllResult(String domain) {
            cacheReads++
            cached
        }

        SearchResponse searchModels(String query, String domain, def sortOrder, Map<String, Integer> pc) {
            searches << [query: query, domain: domain, pagination: pc]
            if (failure) {
                throw failure
            }
            new SearchResponse(results: models(pc.length), totalCount: 2700)
        }

        Map extractSearchModels(SearchResponse response, String domain) {
            [totalCount: response.totalCount as Integer, models: response.results, facets: [],
             facetStats: "[]"]
        }

        void cacheSearchAllResult(Map extracted, String domain) {
            cacheWrites << [extracted: extracted, domain: domain]
        }
    }

    StubSearchService stub = new StubSearchService()

    void setupSpec() {
        defineBeans {
            redisService(RedisService)
        }
    }

    void setup() {
        controller.searchService = stub
        // the action only returns the model for the page when HTML is asked for
        response.format = "html"
    }

    private static List<ModelTransportCommand> models(int count) {
        (1..count).collect {
            new ModelTransportCommand(submissionId: "MODEL$it", name: "Model $it",
                    state: ModelState.PUBLISHED)
        }
    }

    private Map search(Map requestParams) {
        params.putAll([search_block_form: requestParams.query, query: requestParams.query,
                       controller: "Search"] + requestParams)
        controller.search() as Map
    }

    void "a cached all-models result is served without calling EBI Search"() {
        given:
        stub.cached = [matches: 2700, models: models(20), facets: [], facetStats: "[]"]

        when:
        Map result = search(query: "*:*")

        then:
        stub.searches.isEmpty()
        stub.cacheWrites.isEmpty()
        result.matches == 2700
        // 10 is the default page size
        result.models*.submissionId == (1..10).collect { "MODEL$it" }
    }

    void "a larger page is cut from the cached result"() {
        given:
        stub.cached = [matches: 2700, models: models(20), facets: [], facetStats: "[]"]

        when:
        Map result = search(query: "*:*", numResults: "20")

        then:
        stub.searches.isEmpty()
        result.models.size() == 20
    }

    void "a cache miss fetches the whole cacheable page once, caches it and serves the requested page"() {
        when:
        Map result = search(query: "*:*")

        then:
        stub.searches.size() == 1
        stub.searches[0].pagination.start == 0
        stub.searches[0].pagination.length == 20
        stub.cacheWrites.size() == 1
        stub.cacheWrites[0].domain == "biomodels"
        stub.cacheWrites[0].extracted.models.size() == 20
        result.models.size() == 10
        result.matches == 2700
    }

    void "the domain selects the cache entry"() {
        when:
        search(query: "*:*", domain: "biomodels_all")

        then:
        stub.cacheWrites*.domain == ["biomodels_all"]
    }

    void "a request for a later page bypasses the cache"() {
        when:
        search(query: "*:*", offset: "10")

        then:
        stub.cacheReads == 0
        stub.cacheWrites.isEmpty()
        stub.searches.size() == 1
        stub.searches[0].pagination.start == 10
    }

    void "a page larger than the cached one bypasses the cache"() {
        when:
        search(query: "*:*", numResults: "50")

        then:
        stub.cacheReads == 0
        stub.cacheWrites.isEmpty()
        stub.searches[0].pagination.length == 50
    }

    void "sorting by anything but relevance bypasses the cache"() {
        when:
        search(query: "*:*", sort: "name-asc")

        then:
        stub.cacheReads == 0
        stub.cacheWrites.isEmpty()
        stub.searches.size() == 1
    }

    void "other queries never read or write the all-models cache"() {
        when:
        search(query: "glucose")

        then:
        stub.cacheReads == 0
        stub.cacheWrites.isEmpty()
        stub.searches*.query == ["glucose"]
    }

    void "an unavailable search server gives an empty page with a notice, and nothing is cached"() {
        given:
        stub.failure = new SearchUnavailableException("EBI Search did not answer in time", null)

        when:
        Map result = search(query: "*:*")

        then:
        params.searchUnavailable
        result.models.isEmpty()
        result.matches == 0
        stub.cacheWrites.isEmpty()
        response.status == 200
    }

    void "an unavailable search server is reported for other queries too"() {
        given:
        stub.failure = new SearchUnavailableException("EBI Search failed with status 502", null)

        when:
        Map result = search(query: "glucose")

        then:
        params.searchUnavailable
        result.models.isEmpty()
    }

    void "a search that works does not carry the notice"() {
        when:
        search(query: "glucose")

        then:
        !params.searchUnavailable
    }

    void "clients that are not browsers get a 503 when the search server is unavailable"() {
        given:
        stub.failure = new SearchUnavailableException("EBI Search did not answer in time", null)
        response.format = "json"

        when:
        params.putAll([search_block_form: "glucose", query: "glucose", controller: "Search"])
        controller.search()

        then:
        response.status == 503
    }
}
