package net.biomodels.jummp.search

import grails.test.mixin.TestFor
import net.biomodels.jummp.webapp.SearchController
import spock.lang.Specification

/**
 * Created by tnguyen on 09/11/16.
 */
@TestFor(SearchController)
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
}
