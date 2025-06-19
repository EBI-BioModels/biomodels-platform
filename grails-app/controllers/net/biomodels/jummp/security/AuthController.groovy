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
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.annotation.Secured
import net.biomodels.jummp.CommonController
import net.biomodels.jummp.plugins.security.User
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Secured(["IS_AUTHENTICATED_FULLY"])
class AuthController extends CommonController {
    private final Logger LOGGER = LoggerFactory.getLogger(AuthController.class)
    def springSecurityService
    def authService
    def redisService
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
                postUrl  : postUrl,
                tokenName: "2FA",
                user     : userService.currentUser
        ]
        render(view: "form2fa", model: userParams)
    }
    /**
     * <h4>Check the trust devices of the authenticated user</h4>
     *
     * <p>This action is automatically called when the 2FA form is already loaded. It checks and determines to remove
     * or keep the checkbox: Trust this device for 30 days.</p>
     * @return
     */
    def checkTrustDevice() {
        String username = params.username.decodeHTML()
        String deviceInfo = params.deviceInfo.decodeHTML()
        Map result = authService.validateTrustDevice(username, deviceInfo)
        render([message: result["message"], isTrustDeviceExpired: result["expired"]] as JSON)
    }

    /**
     * <h4>Turn on/off two-factor authentication</h4>
     * <p>This action is used to turn on or off the two-step authentication</p>
     * @return
     */
    def toggle2FA() {
        String message = "Under construction"
        int status
        String username = request.getJSON()["username"].decodeHTML()
        boolean checked = request.getJSON()["checked"] as boolean
        String otp = request.getJSON()["otp"].decodeHTML()
        if (!username) {
            message = "You're unauthorised to perform this operation!"
            status = 401
        } else if (!otp) {
            message = "A verification code to confirm the security change is missing!"
            status = 405
        } else {
            Map result = authService.doVerifyOTP(username, otp, request.session.id)
            if (!result["matched"]) {
                message = result["cause"]
                status = 404
            } else {
                if (checked) {
                    message = "You've successfully enabled 2FA!"
                    status = 200
                } else {
                    // delete all OTP generations linked to this user
                    result = authService.disable2FA(username)
                    message = result["cause"]
                    status = 200
                }
            }
        }
        render([message: message, status: status] as JSON)
    }

    /**
     * <h4>Update the trust device on Redis Server</h4>
     * <p>When users click 'Trust this device for 30 days" checkbox, this action is invoked via an async call.</p>
     * @return
     */
    def updateTrustDeviceOnRedis() {
        String deviceInfo = request.getJSON()["deviceInfo"].decodeHTML()
        if (!deviceInfo) {
            render([message: "Cannot store your trust device in our system"] as JSON)
            return
        }
        String username = request.getJSON()["username"].decodeHTML()
        String ipaddr = request.getJSON()["ipaddr"].decodeHTML()
        String type = request.getJSON()["type"].decodeHTML()
        String userAgent = request.getJSON()["userAgent"].decodeHTML()
        String cachedDate = request.getJSON()["cachedDate"].decodeHTML()
        Map mapTrustDevice = [ipaddr: ipaddr, type: type, userAgent: userAgent, cachedDate: cachedDate]
        boolean checked = request.getJSON()["checked"] as boolean
        //authService.updateTrustDevice(checked, username, mapTrustDevice)
        render([message: "OK"] as JSON)
    }

    def verifyOTP() {
        User currentUser = userService.currentUser
        String otp = request.getJSON()["otp"].decodeHTML()
        String postURL, message, cause
        boolean matched = false
        if (!otp) {
            message = "forbidden"
            cause = "You're not allowed to do this operations."
            postURL = createLink(controller: "errors", action: "error403")
        } else {
            message = "valid"
            LOGGER.info "OTP: $otp has been entered by the user: ${currentUser.username}"
            Map m = authService.doVerifyOTP(currentUser.username, otp, session.id)
            matched = m["matched"]
            cause = m["cause"]
            postURL = "/biomodels/user"
        }
        if (matched) {
            session.removeAttribute("enabled2FA")
            String deviceInfo = request.getJSON()["deviceInfo"].decodeHTML()
            if (deviceInfo) {
                boolean isTrustDeviceChecked = request.getJSON()["isTrustDeviceChecked"].decodeHTML().toBoolean()
                Map mInfo = toMapDeviceInfo(deviceInfo)
                authService.updateTrustDevice(isTrustDeviceChecked, currentUser.username, mInfo)
            }
        }
        render([message: message, postUrl: postURL, matched: matched, cause: cause] as JSON)
    }

    def generateOTP() {
        User currentUser = userService.currentUser
        String username = currentUser.username
        String address = "127.0.0.1"
        String sessionId = session.id
        authService.doGenerateOTP(username, address, sessionId)
    }

    private static Map toMapDeviceInfo(final String deviceInfo) {
        // For example: "193.62.199.131|desktop|Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:139.0) Gecko/20100101
        // Firefox/139.0|2025-06-18T22:10:49.561Z"
        if (!deviceInfo) {
            return [:]
        }
        List<String> info = deviceInfo.tokenize("|")
        if (info.isEmpty()) {
            return [:]
        } else {
            Map m = new HashMap()
            m["ipaddr"] = info[0]
            m["type"] = info[1]
            m["userAgent"] = info[2]
            m["cachedDate"] = info[3]
            return m
        }
    }
}
