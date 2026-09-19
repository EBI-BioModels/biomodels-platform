/**
* Copyright (C) 2010-2016 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
**/

package net.biomodels.jummp.plugins.security

import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import spock.lang.Specification
import spock.lang.Unroll

/**
 * JBM-775: after logging in, the user must land on the private model they came from. The production
 * serverURL has no /biomodels context path, so the model link is https://www.biomodels.org/MODEL...
 */
class BioModelsAuthSuccessHandlerSpec extends Specification {
    private static final String PROD = "https://www.biomodels.org"
    private static final String DEV = "http://localhost:8080/biomodels"

    private static BioModelsAuthSuccessHandler handlerFor(String serverURL) {
        def handler = new BioModelsAuthSuccessHandler()
        handler.grailsApplication = [config: [grails: [serverURL: serverURL]]]
        handler.defaultTargetUrl = "/"
        return handler
    }

    private static MockHttpServletRequest requestWith(String previousUrl) {
        def request = new MockHttpServletRequest()
        if (previousUrl != null) {
            request.addParameter("j_previousURL", previousUrl)
        }
        return request
    }

    @Unroll
    void "#serverURL accepts the model page #previousUrl"() {
        expect:
        handlerFor(serverURL).validatedPreviousUrl(requestWith(previousUrl)) == previousUrl

        where:
        serverURL | previousUrl
        PROD      | "https://www.biomodels.org/MODEL2609010001"
        PROD      | "http://www.biomodels.org/MODEL2609010001"     // TLS is terminated by the proxy
        PROD      | "https://WWW.BioModels.org/MODEL2609010001"
        PROD      | "https://www.biomodels.org:443/MODEL2609010001"
        PROD      | "https://www.biomodels.org/BIOMD0000000272"
        PROD      | "https://www.biomodels.org/MODEL2609010001.2"
        PROD      | "https://www.biomodels.org/MODEL2609010001?format=html"
        PROD      | "/MODEL2609010001"
        DEV       | "http://localhost:8080/biomodels/MODEL2609010001"
        DEV       | "/biomodels/MODEL2609010001"
    }

    @Unroll
    void "#serverURL rejects #previousUrl"() {
        expect:
        handlerFor(serverURL).validatedPreviousUrl(requestWith(previousUrl)) == null

        where:
        serverURL | previousUrl
        PROD      | null
        PROD      | ""
        PROD      | "   "
        // open redirects
        PROD      | "https://evil.example/MODEL2609010001"
        PROD      | "https://evil.example/biomodels/MODEL2609010001"
        PROD      | "https://www.biomodels.org.evil.example/MODEL2609010001"
        PROD      | "https://www.biomodels.org@evil.example/MODEL2609010001"
        PROD      | "//evil.example/MODEL2609010001"
        PROD      | "https:///MODEL2609010001"
        PROD      | "javascript:alert(1)//MODEL2609010001"
        PROD      | "https://www.biomodels.org:8443/MODEL2609010001"
        PROD      | "\\\\evil.example\\MODEL2609010001"
        // not a model page of this application
        PROD      | "https://www.biomodels.org/"
        PROD      | "https://www.biomodels.org/user"
        PROD      | "https://www.biomodels.org/admin/MODEL2609010001"
        PROD      | "https://www.biomodels.org/MODEL2609010001/../../admin"
        PROD      | "https://www.biomodels.org/MODEL26090100"
        DEV       | "http://localhost:8080/MODEL2609010001"
        DEV       | "http://localhost:9090/biomodels/MODEL2609010001"
    }

    void "a missing serverURL never yields a redirect target"() {
        expect:
        handlerFor(null).validatedPreviousUrl(requestWith("https://www.biomodels.org/MODEL2609010001")) == null
    }

    void "the login redirects to the private model on production, not to the homepage"() {
        given:
        def handler = handlerFor(PROD)
        String model = "https://www.biomodels.org/MODEL2609010001"

        expect:
        handler.determineTargetUrl(requestWith(model), new MockHttpServletResponse()) == model
    }

    void "the login falls back to the default target when the previous URL is unusable"() {
        expect:
        handlerFor(PROD).determineTargetUrl(requestWith(previousUrl), new MockHttpServletResponse()) == "/"

        where:
        previousUrl << [null, "https://evil.example/MODEL2609010001"]
    }
}
