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
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Secured(["IS_AUTHENTICATED_FULLY"])
class AuthController extends CommonController {
    private final Logger LOGGER = LoggerFactory.getLogger(AuthController.class)

    /**
     * Loads the two-factor authentication form
     * @return
     */
    def load2fa() {
        String postUrl = "${request.contextPath}/${SpringSecurityUtils.securityConfig.textMessage.filterProcessesUrl}"
        postUrl = "/biomodels"
        Map userParams = [
            postUrl: "/biomodels",
            tokenName: "2FA"
        ]
        render(view: "form2FA", model: userParams)
    }

    def verifyOTP() {
        String otp = request.getJSON()["otp"].decodeHTML()
        if (!otp) {
            String postURL = createLink(controller: "errors", action: "error403")
            render([message: "forbidden", postUrl: postURL] as JSON)
        } else {
            println "OTP: $otp"
            LOGGER.info "OTP: $otp"
            session.removeAttribute("enabled2FA")
            render([message: "valid", postUrl: "/biomodels/user"] as JSON)
        }
    }

    def generateOTP() {

    }
}
