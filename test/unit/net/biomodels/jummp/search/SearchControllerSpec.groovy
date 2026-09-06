package net.biomodels.jummp.search

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.web.FiltersUnitTestMixin
import net.biomodels.jummp.core.SearchService
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.utils.redis.RedisService
import net.biomodels.jummp.webapp.SearchController
import spock.lang.Specification

/**
 * Created by tnguyen on 09/11/16.
 */
@TestFor(SearchController)
@TestMixin(FiltersUnitTestMixin)
@Mock([User])
class SearchControllerSpec extends Specification {
    // JBM-705 added a mandatory redisService lookup to CommonController.setConfiguration(), which
    // runs as part of every controller bean's initialisation (not just when an action touches
    // Redis). @TestFor's generated `controller` getter mocks (and so initialises) the controller
    // bean in an instance-level @Before fixture, which runs before Spock's own setup() - so the
    // bean has to be registered at the class level (setupSpec()), not per-test, or it is still
    // missing when the controller bean gets created.
    void setupSpec() {
        defineBeans {
            redisService(RedisService)
        }
    }

    void setup() {
    }

    void cleanup() {

    }

    void "test index request"() {
        given: 'SearchController'
        // declare an object referring to this controller or use the default 'controller' object
        // As the result, the test class has to name as the pattern 'NameControllerTests'
        def searchController = new SearchController()
        when: 'Homepage/Index Method is called'
        controller.index()
        then: 'check redirect url and error message'
        "abcdef" != response.text
    }

    void "test search action is filtered"() {
        given: "given parameters to search action"
        /**
         * The search action invokes searchService.searchModels, therefore,
         * we have to mock this method in a mocked service
         */
        def searchService = mockFor(SearchService)
        searchService.metaClass.searchModels = { String q, String d, def so, def pc ->
            return new SearchResponse(results: null, facets: null, totalCount: 0)
        }
        // doSearch() started chaining extractSearchModels() onto the searchModels() result -
        // without stubbing it too, the real (unmocked) implementation runs and throws.
        searchService.metaClass.extractSearchModels = { def response, String d ->
            return [totalCount: 0, models: [], facets: [], facetStats: ""]
        }
        controller.searchService = searchService

        String query = "<strong>metabolism</strong>"
        params.query = query
        params.controller = "Search"

        // the encoding under test happens in ParameterFilters.before, not in the controller
        // action itself - withFilters wraps the call so that filter actually runs.
        // ParameterFilters (grails-app/conf/ParameterFilters.groovy) is in the default package;
        // a bare reference from this named-package spec resolves as a runtime property lookup
        // rather than a compile-time class literal (there is no import syntax that crosses the
        // default-package boundary), so it is looked up reflectively instead.
        mockFilters(Class.forName("ParameterFilters"))
        webRequest.controllerName = "Search"
        webRequest.actionName = "search"

        when: "the filter chain's before phase runs, ahead of the action"
        getCompositeInterceptor().preHandle(request, response, this)

        then: "the raw query was HTML-encoded by the global sanitizing filter"
        params.query == "&lt;strong&gt;metabolism&lt;/strong&gt;"

        when: "the search action itself then runs"
        controller.search()

        then: "search() decodes the query back to its original form for its own processing " +
                "(see the comment in SearchController.doSearch), so the round trip is lossless"
        params.query == query
        response.text != null
    }
}
