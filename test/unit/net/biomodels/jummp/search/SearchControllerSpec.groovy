package net.biomodels.jummp.search

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import net.biomodels.jummp.core.SearchService
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.webapp.SearchController
import spock.lang.Specification

/**
 * Created by tnguyen on 09/11/16.
 */
@TestFor(SearchController)
@Mock([User])
class SearchControllerSpec extends Specification {
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
        controller.searchService = searchService

        String query = "<strong>metabolism</strong>"
        params.query = query
        params.controller = "Search"

        when: "call the search action"
        withFilters(action: "search") {
            controller.search()
        }

        then: "the query was encoded"
        params.query == "&lt;strong&gt;metabolism&lt;/strong&gt;"
        response.text != null
    }
}
