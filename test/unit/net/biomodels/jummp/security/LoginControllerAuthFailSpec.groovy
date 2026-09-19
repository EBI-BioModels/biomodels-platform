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

package net.biomodels.jummp.security

import grails.test.mixin.TestFor
import net.biomodels.jummp.plugins.security.BioModelsAuthFailureHandler as FailureHandler
import net.biomodels.jummp.utils.redis.RedisService
import spock.lang.Specification

/**
 * JBM-775: a failed login must not make the user forget the model page they wanted. The failure url is fixed,
 * and the Referer header of the redirected login form is the login page itself, so the second attempt
 * used to end up on the homepage.
 */
@TestFor(LoginController)
class LoginControllerAuthFailSpec extends Specification {
    private static final String MODEL = "https://www.biomodels.org/MODEL2609010001"

    // CommonController.setConfiguration() looks redisService up while the controller bean is initialised,
    // which happens before setup() runs, so the bean has to be defined at class level.
    void setupSpec() {
        defineBeans {
            redisService(RedisService)
        }
    }

    void "authfail hands the remembered model page back to the login form"() {
        given:
        session[FailureHandler.FAILED_LOGIN_PREVIOUS_URL] = MODEL
        params.login_error = "1"

        when:
        controller.authfail()

        then:
        response.redirectedUrl.startsWith("/login/auth?")
        response.redirectedUrl.contains("login_error=1")
        response.redirectedUrl.contains("previousURL=" + URLEncoder.encode(MODEL, "UTF-8"))
    }

    void "the remembered page is handed back only once"() {
        given:
        session[FailureHandler.FAILED_LOGIN_PREVIOUS_URL] = MODEL

        when:
        controller.authfail()

        then:
        session.getAttribute(FailureHandler.FAILED_LOGIN_PREVIOUS_URL) == null
    }

    void "authfail does not invent a previous page when none was remembered"() {
        when:
        controller.authfail()

        then:
        !response.redirectedUrl.contains("previousURL")
    }

    void "a previousURL sent to authfail by the client does not replace the remembered one"() {
        given:
        session[FailureHandler.FAILED_LOGIN_PREVIOUS_URL] = MODEL
        params.previousURL = "https://evil.example/MODEL2609010001"

        when:
        controller.authfail()

        then:
        response.redirectedUrl.contains("previousURL=" + URLEncoder.encode(MODEL, "UTF-8"))
        !response.redirectedUrl.contains("evil.example")
    }
}
