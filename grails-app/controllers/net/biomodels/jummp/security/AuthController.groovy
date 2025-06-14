/**
* Copyright (C) 2010-2025 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.plugin.springsecurity.SpringSecurityUtils
import net.biomodels.jummp.CommonController
import net.biomodels.jummp.plugins.security.User
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Secured(["IS_AUTHENTICATED_FULLY"])
class AuthController extends CommonController {
    private final Logger LOGGER = LoggerFactory.getLogger(AuthController.class)
    def springSecurityService
    def authService
    def userService

    /**
     * Loads the two-factor authentication form
     * @return
     */
    def load2fa() {
        if (!session.getAttribute("enabled2FA") && springSecurityService.isLoggedIn()) {
            forward(plugin: "jummp-plugin-web-application", controller: "errors", action: "error405")
            return
        }
        String postUrl = "${request.contextPath}/${SpringSecurityUtils.securityConfig.textMessage.filterProcessesUrl}"
        postUrl = "/biomodels"
        Map userParams = [
            postUrl: postUrl,
            tokenName: "2FA",
            user: userService.currentUser
        ]
        render(view: "form2fa", model: userParams)
    }

    def verifyOTP() {
        User currentUser = userService.currentUser
        String otp = request.getJSON()["otp"].decodeHTML()
        String postURL
        String message
        boolean matched = false
        if (!otp) {
            message = "forbidden"
            postURL = createLink(controller: "errors", action: "error403")
        } else {
            message = "valid"
            LOGGER.info "OTP: $otp has been entered by the user: ${currentUser.username}"
            matched = authService.doVerifyOTP(currentUser.username, otp, session.id)
            postURL = "/biomodels/user"
        }
        if (matched) {
            session.removeAttribute("enabled2FA")
        }
        render([message: message, postUrl: postURL, matched: matched] as JSON)
    }

    def generateOTP() {
        User currentUser = userService.currentUser
        String username = currentUser.username
        String address = "127.0.0.1"
        String sessionId = session.id
        authService.doGenerateOTP(username, address, sessionId)
    }
}
