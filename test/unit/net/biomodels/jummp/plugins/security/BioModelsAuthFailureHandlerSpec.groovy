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
import spock.lang.Specification

/**
 * JBM-775: the page the user wanted has to survive a failed login attempt.
 */
class BioModelsAuthFailureHandlerSpec extends Specification {
    private static final String KEY = BioModelsAuthFailureHandler.FAILED_LOGIN_PREVIOUS_URL

    void "the submitted j_previousURL is kept in the session"() {
        given:
        def request = new MockHttpServletRequest()
        request.addParameter("j_previousURL", "  https://www.biomodels.org/MODEL2609010001 ")

        when:
        new BioModelsAuthFailureHandler().rememberPreviousUrl(request)

        then:
        request.session.getAttribute(KEY) == "https://www.biomodels.org/MODEL2609010001"
    }

    void "nothing is stored when there is no usable j_previousURL"() {
        given:
        def request = new MockHttpServletRequest()
        if (value != null) {
            request.addParameter("j_previousURL", value)
        }

        when:
        new BioModelsAuthFailureHandler().rememberPreviousUrl(request)

        then:
        request.session.getAttribute(KEY) == null

        where:
        value << [null, "", "   "]
    }
}
