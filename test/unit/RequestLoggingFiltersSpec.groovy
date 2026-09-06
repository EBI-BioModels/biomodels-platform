/**
* Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
* Deutsches Krebsforschungszentrum (DKFZ)
*
* This file is part of Jummp.
*
* Jummp is free software; you can redistribute it and/or modify it under the
* terms of the GNU Affero General Public License as published by the Free
* Software Foundation; either version 3 of the License, or (at your option) any
* later version.
*
* Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
* A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
* details.
*
* You should have received a copy of the GNU Affero General Public License along
* with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
*
* Additional permission under GNU Affero GPL version 3 section 7
*
* If you modify Jummp, or any covered work, by linking or combining it with
* Grails (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Grails used as well as
* that of the covered work.}
**/




// No package declaration: RequestLoggingFilters (grails-app/conf/RequestLoggingFilters.groovy)
// is itself declared in the default package, and @TestFor/@Mock's annotation-value class
// resolution does not cross the default-package boundary - a bare class reference from a named
// package fails to resolve at annotation-processing time, which is what broke this spec
// originally (it lived under test/unit/biomodels with `package biomodels`).

import grails.test.mixin.TestFor
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Grails 2.5.5's @Mock annotation does not support the Filters artefact type at all - any
 * @Mock(SomeFilters) fails compilation with "annotation expects a class or a list of classes to
 * mock", regardless of what SomeFilters actually contains (confirmed against
 * org.codehaus.groovy.grails.compiler.injection.test.MockTransformation, whose artefact map has
 * no Filters entry). Filters classes have to be unit-tested through @TestFor instead, which
 * grails-plugin-testing maps to FiltersUnitTestMixin (mockFilters/withFilters) by matching the
 * "Filters" class name suffix - see grails.test.mixin.web.FiltersUnitTestMixin.
 */
@TestFor(RequestLoggingFilters)
class RequestLoggingFiltersSpec extends Specification {

    @Unroll
    void "before records a startTime attribute when the query string is #queryString"() {
        given:
        request.queryString = queryString

        when:
        withFilters(controller: "test", action: "index") { "ok" }

        then:
        (request.getAttribute("startTime") != null) == shouldMatch

        where:
        queryString        | shouldMatch
        "format=json"      | true
        "foo=bar&format="  | true
        "/api/models"      | true
        null               | false
        "foo=bar"          | false
    }

    void "afterView does not throw computing a duration when the request never matched"() {
        // Regression guard: afterView does `request.getAttribute('startTime') as long`, which
        // throws a GroovyCastException on a null attribute unless it is gated behind the same
        // match condition as `before`. If a future edit drops that guard, every non-API request
        // would start throwing here instead of just skipping the timing log.
        given:
        request.queryString = null

        when:
        withFilters(controller: "test", action: "index") { "ok" }

        then:
        noExceptionThrown()
        request.getAttribute("startTime") == null
    }
}
