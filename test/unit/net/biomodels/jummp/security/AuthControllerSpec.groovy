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
* Grails, JUnit (or a modified version of that library), containing parts
* covered by the terms of Common Public License, Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Grails, JUnit used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.security

import grails.test.mixin.TestFor
import net.biomodels.jummp.plugins.security.BioModelsAuthSuccessHandler
import net.biomodels.jummp.utils.redis.RedisService
import spock.lang.Specification
import spock.lang.Unroll

/**
 * JBM-789: after a verified OTP the user has to land where a login without 2FA lands, on the model page they came
 * for or on the default target. The application runs at the root of the host on production but under /biomodels in
 * development, so the redirect must not contain a fixed path.
 */
@TestFor(AuthController)
class AuthControllerSpec extends Specification {
    private static final String TARGET_KEY = BioModelsAuthSuccessHandler.POST_LOGIN_TARGET_URL

    /** CommonController looks the bean up when it is created; the real one would connect to a Redis server. */
    static class NoRedisService extends RedisService {
        @Override
        void setConfiguration(ConfigObject co) {}

        @Override
        void destroy() {}
    }

    /* CommonController.setConfiguration reads these and looks the bean up while the controller is being created,
     * which the mixin does before setup() runs. */
    static doWithConfig = { c ->
        c.grails.serverURL = "https://www.biomodels.org"
        c.jummp.branding.style = "biomodels"
        c.jummp.context.help.root = "https://www.biomodels.org/help"
        c.jummp.model.ftp.location = ""
    }
    static doWithSpring = {
        redisService(NoRedisService)
    }

    def setup() {
        controller.userService = [currentUser: [username: "curator"]]
        controller.authService = [doVerifyOTP: { String username, String otp, String sessionId ->
            otp == "123456" ? [matched: true] : [matched: false, cause: "OTP mismatch. Try again or request a new one."]
        }]
        session.setAttribute("enabled2FA", true)
    }

    private void submit(String otp) {
        request.method = "POST"
        request.json = [otp: otp, deviceInfo: "", isTrustDeviceChecked: "false"]
        controller.verifyOTP()
    }

    @Unroll
    void "a verified OTP on context path '#contextPath' without a kept target ends on #expected"() {
        given:
        request.contextPath = contextPath

        when:
        submit("123456")

        then:
        response.json.matched
        response.json.postUrl == expected
        !response.json.postUrl.contains("/biomodels/user")

        where:
        contextPath  || expected
        ""           || "/"                  // production
        "/biomodels" || "/biomodels/"        // development
    }

    void "a verified OTP sends the user to the model page kept at login"() {
        given:
        session.setAttribute(TARGET_KEY, "https://www.biomodels.org/MODEL2609010001")

        when:
        submit("123456")

        then:
        response.json.postUrl == "https://www.biomodels.org/MODEL2609010001"
    }

    void "the default landing page follows the configured default target of a login"() {
        given:
        grailsApplication.config.grails.plugin.springsecurity.successHandler.defaultTargetUrl = "/models"
        request.contextPath = "/biomodels"

        when:
        submit("123456")

        then:
        response.json.postUrl == "/biomodels/models"
    }

    void "a verified OTP ends the pending 2FA state and forgets the kept target"() {
        given:
        session.setAttribute("pendingEnrollment", true)
        session.setAttribute(TARGET_KEY, "/")

        when:
        submit("123456")

        then:
        session.getAttribute("enabled2FA") == null
        session.getAttribute("pendingEnrollment") == null
        session.getAttribute(TARGET_KEY) == null
    }

    void "a wrong OTP leaves the session pending and keeps the target"() {
        given:
        session.setAttribute(TARGET_KEY, "/")

        when:
        submit("000000")

        then:
        !response.json.matched
        response.json.cause == "OTP mismatch. Try again or request a new one."
        session.getAttribute("enabled2FA")
        session.getAttribute(TARGET_KEY) == "/"
    }
}
