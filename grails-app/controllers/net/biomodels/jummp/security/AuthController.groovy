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
import groovy.json.JsonSlurper
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
        String message
        List<String> devices
        if (!deviceInfo) {
            message = "No information of your trust device provided."
            render([message: message, isTrustDeviceExpired: true] as JSON)
            return
        } else {
            devices = deviceInfo.tokenize("|")
            if (devices.isEmpty()) {
                message = "An error happened when tokenising the device info."
                render([message: message, isTrustDeviceExpired: true] as JSON)
                return
            }
        }
        Set<String> trustDevices = redisService.doRedisSMembers("trustdevices:$username")
        def parser = new JsonSlurper()
        def json
        String matched = trustDevices.find {
            json = parser.parseText(it)
            json["ipaddr"] == devices[0] && json["type"] == devices[1] && json["userAgent"] == devices[2]
        }
        boolean expired
        if (matched) {
            json = parser.parseText(matched)
            expired = authService.isTrustDeviceExpired(json["cachedDate"] as String)
            message = "This trust device has ${expired ? 'expired' : 'unexpired yet'}."
        } else {
            message = "No information about this device."
            expired = true
        }
        render([message: message, isTrustDeviceExpired: expired] as JSON)
    }

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
        authService.updateTrustDevice(checked, username, mapTrustDevice)
        render([message: "will be implemented"] as JSON)
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
            boolean isTrustDeviceChecked = request.getJSON()["isTrustDeviceChecked"].decodeHTML() as boolean
            String deviceInfo = request.getJSON()["deviceInfo"].decodeHTML()
            Map mInfo = toMapDeviceInfo(deviceInfo)
            authService.updateTrustDevice(isTrustDeviceChecked, currentUser.username, mInfo)
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
