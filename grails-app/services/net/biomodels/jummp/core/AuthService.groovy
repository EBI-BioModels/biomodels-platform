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





package net.biomodels.jummp.core

import grails.transaction.Transactional
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import net.biomodels.jummp.core.constants.BioModels
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.security.IAuthService
import net.biomodels.jummp.security.TwoFactorAuth
import net.biomodels.jummp.security.TwoFactorAuth as TFA
import net.biomodels.jummp.utils.MathUtils
import net.biomodels.jummp.utils.TimeUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.text.SimpleDateFormat

@Transactional
class AuthService implements IAuthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class)
    /** How long a one-time passcode can be used after it was issued. */
    static final long OTP_VALIDITY_MILLIS = 15 * 60 * 1000L

    /**
     * Tells whether a one-time passcode issued at the given time can still be used.
     *
     * <p>The elapsed time is compared as a whole. Looking only at the minutes and seconds of a TimeCategory duration
     * ignores its hours and days, which made a code valid again for the first 15 minutes of every hour after it was
     * issued (JBM-790).</p>
     */
    static boolean isOtpValid(final Date issuedDate, final Date now = new Date()) {
        return issuedDate != null && now.time - issuedDate.time < OTP_VALIDITY_MILLIS
    }
    def grailsApplication
    def redisService
    def userService
    def mailingService

    @Override
    List<TwoFactorAuth> findAll(final String username, final String otp, final String sessionId) {
        String queryString = """select id from TwoFactorAuth t where t.user.username = :username and t.sessionId = \
:sessionId and t.otp = :otp"""
        List iDs = TFA.executeQuery(queryString, [username: username, sessionId: sessionId, otp: otp])
        List<TFA> results = new ArrayList<>()
        iDs.each {
            results.add(TFA.get(it))
        }
        results
    }

    @Override
    boolean isTrustDeviceExpired(String strDateTime) {
        def sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"))
        def cachedDate = sdf.parse(strDateTime)
        double days = TimeUtils.diffTwoDates(new Date(), cachedDate, "D")
        return days > 30
    }

    /**
     * <h4>Validate the trust device of a given user</h4>
     * <p>This service is used to check the device of a given user needing to require two step verification.</p>
     * @param username The username of the user in question
     * @param deviceInfo The string representing the information such IP address, Mobile or Desktop, User Agent,...
     * of the user's device.
     * @return a map including two elements: message showing the information and expired indicating true/false
     */
    @Override
    Map validateTrustDevice(final String username, final String deviceInfo) {
        String message
        boolean expired = false
        List devices = new ArrayList()
        if (!deviceInfo) {
            message = "No information of your trust device provided."
            expired = true
        } else {
            devices = deviceInfo.tokenize("|")
            if (devices.isEmpty()) {
                message = "An error happened when tokenising the device info."
                expired = true
            } else {
                message = ""
            }
        }
        if (devices) {
            Set<String> trustDevices = redisService.doRedisSMembers("trustdevices:$username")
            def parser = new JsonSlurper()
            def json
            String matched = trustDevices.find {
                json = parser.parseText(it)
                json["ipaddr"] == devices[0] && json["type"] == devices[1] && json["userAgent"] == devices[2]
            }
            if (matched) {
                json = parser.parseText(matched)
                expired = isTrustDeviceExpired(json["cachedDate"] as String)
                message = "This trust device has ${expired ? 'expired' : 'unexpired yet'}."
            } else {
                message = "No information about this device"
                expired = true
            }
        }
        return [message: message, expired: expired]
    }

    /**
     * <h4>Determine a given user enabled 2FA or not</h4>
     * <p>This service is used to check a given user enabling 2FA or not.</p>
     * @param username The username of the given user
     * @return true|false
     */
    @Override
    boolean is2FAEnabled(String username) {
        String queryString = "select id from TwoFactorAuth t where t.user.username = :username"
        List results = TFA.executeQuery(queryString, [username: username])
        return !results.isEmpty()
    }

    /**
     * <h4>Disable two-factor authentication of a given user</h4>
     * <p>This method is used to disable the 2FA of a given user.
     *
     * @param username A string denoting the username of the given user
     * @return A map including the cause/message and status
     */
    @Override
    Map disable2FA(String username) {
        String cause = "2FA has been disabled"
        boolean status = true
        String queryString = "select id from TwoFactorAuth t where t.user.username = :username"
        List<Long> results = TwoFactorAuth.executeQuery(queryString, [username: username]) as List<Long>
        if (results.isEmpty()) {
            cause = "Not found - 2FA is on"
        } else {
            for (long deletedId in results) {
                try {
                    TFA.where { id == deletedId }.deleteAll()
                    TFA.withSession { it.flush() }
                    LOGGER.info("Deleted the TFA record [id: ${deletedId}, username: ${username}] successfully.")
                } catch (Exception e) {
                    status = false
                    cause = """An error happened when trying to disable 2FA. Please try later or contact us for \
further support"""
                    LOGGER.error("Cannot delete the TFA record [id: ${deletedId}, username: ${username}] due to ${e.message}")
                }
            }
        }
        return [cause: cause, status: status]
    }

    @Override
    String doGenerateOTP(final String username, final String remoteAddress, final String sessionId) {
        final User USER = userService?.currentUser
        TFA auth = null
        String queryString = "select id from TwoFactorAuth t where t.user.id = :userId and t.sessionId = :sessionId"
        List results = TFA.executeQuery(queryString, [userId: USER.id, sessionId: sessionId])
        if (!results.isEmpty()) {
            auth = TFA.get(results.first())
        }
        // Extract strings eagerly while the Hibernate session is still open
        final String realName = USER.person.userRealName
        final String toEmail = USER.email
        if (auth) {
            if (isOtpValid(auth.issuedDate)) {
                LOGGER.info("Reused OTP for username: $username; address: $remoteAddress; session: $sessionId")
                final String existingOtp = auth.otp
                Thread.start { emailOTP(realName, toEmail, existingOtp) }
                return auth.otp
            }
        }
        String otp = MathUtils.generatePassword('0123456789', 6)
        LOGGER.info("Created new OTP for username: $username; address: $remoteAddress; session: $sessionId")
        auth = new TFA(user: USER, sessionId: sessionId, otp: otp, issuedDate: new Date())
        if (!auth.save(flush: true)) {
            LOGGER.error("Cannot create a new OTP requested by user $username (sessionId: $sessionId).")
            return ""
        }
        Thread.start { emailOTP(realName, toEmail, otp) }
        LOGGER.info("OTP email dispatched asynchronously for user $username")
        return otp
    }

    @Override
    Map doVerifyOTP(final String username, final String otp, final String sessionId) {
        LOGGER.info("Verifying OTP for user: $username at session: $sessionId")
        List<TFA> results = findAll(username, otp, sessionId)
        if (results.isEmpty()) {
            LOGGER.debug("No OTP match for user: $username at session: $sessionId")
            return [matched: false, cause: "OTP mismatch. Try again or request a new one."]
        }
        TFA first = results?.first()
        if (first) {
            boolean valid = isOtpValid(first.issuedDate)
            if (!valid) {
                return [matched: valid, cause: "OTP expired. You can request a new one."]
            }
            consume(first)
            return [matched: valid]
        } else {
            LOGGER.debug("No OTP match for user: $username at session: $sessionId")
            return [matched: false, cause: "OTP doesn't exist. Check it in your email again."]
        }
    }

    /**
     * Makes a code that has just been accepted unusable, so that it can be entered only once, as the OTP form tells
     * the user.
     *
     * <p>The row is kept and only expired. {@link #is2FAEnabled} decides that a user has 2FA on from the rows that
     * exist, so deleting it could turn their 2FA off.</p>
     */
    private void consume(final TFA code) {
        code.issuedDate = new Date(System.currentTimeMillis() - OTP_VALIDITY_MILLIS)
        if (!code.save(flush: true)) {
            LOGGER.error("Cannot expire the verification code that has just been used: ${code.errors}")
        }
    }

    void updateTrustDevice(final boolean checked, final String username, final Map deviceInfo) {
        def data = new JsonBuilder(deviceInfo).toString()
        if (checked) {
            redisService.doRedisSAdd("trustdevices:$username", data)
        } else {
            Set<String> trustDevices = redisService.doRedisSMembers("trustdevices:$username")
            def parser = new JsonSlurper()
            def json
            for (String device : trustDevices) {
                json = parser.parseText(device)
                if (json["ipaddr"] == deviceInfo["ipaddr"]
                        && json["type"] == deviceInfo["type"]
                        && json["userAgent"] == deviceInfo["userAgent"]) {
                    redisService.doRedisSRem("trustdevices:$username", device)
                }
            }
        }
    }

    /**
     * <h4>Email to the users when they enable or disable 2FA</h4>
     * <p>When enabling or disabling 2FA successfully, an email will be sent to the user to confirm the activity.</p>
     * @param USER A {@link User} object indicating who has changed the 2FA
     * @param enabled2FA true|false indicating enable or disable the 2FA
     */
    void emailWhenToggle2FA(final User USER, final boolean enabled2FA) {
        final String SENDER = grailsApplication.config.jummp.security.registration.email.sender
        final String SVR_URL = grailsApplication.config.grails.serverURL
        String SUBJECT = enabled2FA
            ? "[BioModels] Two-Factor Authentication Enabled"
            : "[BioModels] Two-Factor Authentication Disabled"
        String MAIN_TEXT
        if (enabled2FA) {
            MAIN_TEXT = """
      <p style="margin:0 0 16px;">Two-Factor Authentication has been enabled on your BioModels account
      (<a href="mailto:${USER.email}" style="color:#0F5CB1;">${USER.email}</a>).
      Each time you sign in from a new or unrecognised device, you will be prompted to verify your identity
      with a one-time code sent to your registered email address.</p>
      <p style="margin:0 0 16px;">You can <a href="${SVR_URL}/user" style="color:#0F5CB1;">review or update your 2FA settings</a> at any time from your profile.</p>"""
        } else {
            MAIN_TEXT = """
      <p style="margin:0 0 16px;">Two-Factor Authentication has been disabled on your BioModels account. You can now sign in using your username and password only.</p>
      <p style="margin:0 0 16px;">If you change your mind, you can <a href="${SVR_URL}/user" style="color:#0F5CB1;">re-enable 2FA</a> from your profile at any time.</p>"""
        }
        String INNER = """
      <p style="margin:0 0 16px;">Dear ${USER.person.userRealName},</p>
      ${MAIN_TEXT}
      <p style="margin:0 0 16px;">If you did not make this change, please <a href="mailto:${SENDER}" style="color:#0F5CB1;">contact us</a> immediately, as your account may be at risk.</p>
      <p style="margin:0 0 16px;">Kind regards,<br/><strong>The BioModels Team</strong><br/>
        <a href="${BioModels.BM_ROOT_URL}" style="color:#0F5CB1;">${BioModels.BM_ROOT_URL}</a>
      </p>"""
        String FOOTER_NOTE = """You are receiving this email because you have an account on
      <a href="${BioModels.BM_ROOT_URL}" style="color:#0F5CB1;">BioModels</a>,
      a repository of mathematical models of biological processes.
      This is an automatically generated email &mdash; replies are not monitored."""
        String BODY = mailingService.wrapHtml(INNER, [footerNote: FOOTER_NOTE, showMaintainer: true])
        userService.sendEmail(USER, BODY, SUBJECT)
    }

    private void emailOTP(final String realName, final String toEmail, final String OTP) {
        final String CONTACT = grailsApplication.config.jummp.security.registration.email.contact
        final String INNER = """
      <p style="margin:0 0 16px;">Dear ${realName},</p>
      <p style="margin:0 0 16px;">We received a request to verify your identity on your BioModels account. Please use the code below to complete your request.</p>
      <div style="background-color:#f0f4fa;border-left:4px solid #072C55;padding:20px;margin:0 0 20px;text-align:center;border-radius:0 4px 4px 0;">
        <div style="font-size:36px;font-weight:bold;letter-spacing:8px;color:#072C55;">$OTP</div>
      </div>
      <p style="margin:0 0 16px;">This code is valid for <strong>15 minutes</strong>. For your security, do not share it with anyone, including BioModels staff.</p>
      <p style="margin:0 0 16px;">If you did not request this code, please <a href="mailto:${CONTACT}" style="color:#0F5CB1;">contact us</a> immediately, as your account may be at risk.</p>
      <p style="margin:0 0 16px;">Kind regards,<br/><strong>The BioModels Team</strong><br/>
        <a href="${BioModels.BM_ROOT_URL}" style="color:#0F5CB1;">${BioModels.BM_ROOT_URL}</a>
      </p>"""
        final String FOOTER_NOTE = """You are receiving this email because you have an account on
      <a href="${BioModels.BM_ROOT_URL}" style="color:#0F5CB1;">BioModels</a>,
      a repository of mathematical models of biological processes.
      This is an automatically generated email &mdash; replies are not monitored."""
        final String BODY = mailingService.wrapHtml(INNER, [footerNote: FOOTER_NOTE, showMaintainer: true])
        final String SUBJECT = "[BioModels] Your Verification Code"
        userService.sendEmail(toEmail, BODY, SUBJECT)
    }
}
