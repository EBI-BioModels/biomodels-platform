package net.biomodels.jummp.core

import net.biomodels.jummp.core.model.ModelFormatTransportCommand
import net.biomodels.jummp.core.model.ModelState
import net.biomodels.jummp.core.model.ModelTransportCommand
import spock.lang.Specification
import uk.ac.ebi.ddi.ebe.ws.dao.model.common.Facet
import uk.ac.ebi.ddi.ebe.ws.dao.model.common.FacetValue

/**
 * The Redis cache of the *:* search result (JBM-820).
 */
class SearchServiceCacheSpec extends Specification {

    /** Stands in for RedisService, whose methods are static and cannot be mocked. */
    static class FakeRedis {
        Map<String, Map<String, String>> store = [:]
        int writes = 0
        boolean exists(String key) { store.containsKey(key) }
        Map doRedisHGetAll(String key) { store[key] }
        void doRedisHSet(String key, Map<String, String> data) {
            store[key] = new HashMap<>(data)
            writes++
        }
    }

    FakeRedis redis = new FakeRedis()
    SearchService service = new SearchService(redisService: redis)

    private static Map extracted(int count = 2) {
        def models = (1..count).collect {
            new ModelTransportCommand(submissionId: "MODEL$it", name: "Model $it",
                    submissionDate: new Date(1000L * it), lastModifiedDate: new Date(2000L * it),
                    state: ModelState.PUBLISHED,
                    format: new ModelFormatTransportCommand(name: "SBML", formatVersion: "L3V1"))
        }
        def facet = new Facet(id: "modelformat", label: "Model format")
        facet.facetValues = [new FacetValue(label: "SBML", value: "SBML", count: "$count")] as FacetValue[]
        [totalCount: 2700, models: models, facets: [facet], facetStats: '[{"id":"modelformat"}]']
    }

    void "a cached result is read back as it was written"() {
        when:
        service.cacheSearchAllResult(extracted(), "biomodels")
        Map cached = service.retrieveCachedSearchAllResult("biomodels")

        then:
        cached.matches == 2700
        cached.models*.submissionId == ["MODEL1", "MODEL2"]
        cached.models[1].lastModifiedDate == new Date(4000L)
        cached.facets.size() == 1
        cached.facets[0].facetValues[0].value == "SBML"
        cached.facetStats == '[{"id":"modelformat"}]'
    }

    void "each domain has its own entry"() {
        when:
        service.cacheSearchAllResult(extracted(2), "biomodels")
        service.cacheSearchAllResult(extracted(3), "biomodels_all")

        then:
        service.retrieveCachedSearchAllResult("biomodels").models.size() == 2
        service.retrieveCachedSearchAllResult("biomodels_all").models.size() == 3
        service.retrieveCachedSearchAllResult("biomodels_autogen") == [:]
    }

    void "an empty result does not replace a good cache entry"() {
        given:
        service.cacheSearchAllResult(extracted(), "biomodels")

        when:
        service.cacheSearchAllResult([totalCount: 0, models: [], facets: [], facetStats: ""], "biomodels")

        then:
        redis.writes == 1
        service.retrieveCachedSearchAllResult("biomodels").models.size() == 2
    }

    void "a missing entry is a cache miss"() {
        expect:
        service.retrieveCachedSearchAllResult("biomodels") == [:]
    }

    void "an entry in the old id|name format is a cache miss, not an error"() {
        given:
        redis.store["search-result:all:biomodels"] = [totalCount: "5", facetStats: "[]",
                models: "MODEL1|Glycolysis", facets: "format|Format"]

        expect:
        service.retrieveCachedSearchAllResult("biomodels") == [:]
    }

    void "without Redis nothing is read or written"() {
        given:
        def noRedis = new SearchService()

        when:
        noRedis.cacheSearchAllResult(extracted(), "biomodels")

        then:
        noExceptionThrown()
        noRedis.retrieveCachedSearchAllResult("biomodels") == [:]
    }
}
