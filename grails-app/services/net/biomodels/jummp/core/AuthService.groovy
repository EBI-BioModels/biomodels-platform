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
import groovy.time.TimeCategory
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
    def grailsApplication
    def redisService
    def userService

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
            boolean valid
            use(TimeCategory) {
                def duration = new Date() - auth.issuedDate
                // valid if the issued date is not over 15 minutes
                valid = duration.minutes*60 + duration.seconds < 15*60
            }
            if (valid) {
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
            boolean valid = false
            use(TimeCategory) {
                def duration = new Date() - first.issuedDate
                // println "Days: ${duration.days}, Hours: ${duration.hours}, etc."
                // valid if the issued date is not over 15 minutes
                valid = duration.minutes*60 + duration.seconds < 15*60
            }
            if (!valid) {
                // make it expired because it has already been used. Should we?
                return [matched: valid, cause: "OTP expired. You can request a new one."]
            }
            return [matched: valid]
        } else {
            LOGGER.debug("No OTP match for user: $username at session: $sessionId")
            return [matched: false, cause: "OTP doesn't exist. Check it in your email again."]
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
        String SUBJECT = "[BioModels] Two-Factor Authentication turned"
        if (enabled2FA) {
            SUBJECT = "$SUBJECT on"
        } else {
            SUBJECT = "$SUBJECT off"
        }
        String MAIN_TEXT
        if (enabled2FA) {
            MAIN_TEXT = """
      <p style="margin:0 0 16px;">Your BioModels account <a href="mailto:${USER.email}" style="color:#0F5CB1;">${USER.email}</a> is now protected with Two-Factor Authentication.
      When you sign in on a new or untrusted device, you will need your second factor to verify your identity.</p>
      <p style="margin:0 0 16px;">You can <a href="${SVR_URL}/user" style="color:#0F5CB1;">review your 2FA settings</a> at any time to make changes.</p>"""
        } else {
            MAIN_TEXT = """
      <p style="margin:0 0 16px;">Two-Factor Authentication has been disabled on your BioModels account. You will no longer need a second factor to sign in.</p>"""
        }
        String BODY = """
<div style="background-color:#f4f4f4;margin:0;padding:32px 0;font-family:Arial,Helvetica,sans-serif;color:#333333;">
  <div style="max-width:620px;margin:0 auto;background-color:#ffffff;border-radius:4px;overflow:hidden;box-shadow:0 2px 6px rgba(0,0,0,0.10);">
    <div style="background-color:#ED6B21;height:5px;"></div>
    <div style="background-color:#072C55;padding:24px 32px 20px;">
      <div style="font-size:22px;font-weight:bold;color:#ffffff;letter-spacing:0.5px;">BioModels</div>
      <div style="font-size:12px;color:rgba(255,255,255,0.75);margin-top:4px;letter-spacing:0.3px;">Laboratory for Systems Medicine &bull; University of Florida</div>
    </div>
    <div style="padding:32px;font-size:15px;line-height:1.7;color:#333333;">
      <p style="margin:0 0 16px;">Dear ${USER.person.userRealName},</p>
      ${MAIN_TEXT}
      <p style="margin:0 0 16px;">If you did not perform this action, please <a href="mailto:${SENDER}" style="color:#0F5CB1;">contact us</a> immediately.</p>
      <p style="margin:0 0 16px;">Kind regards,<br/><strong>The BioModels Team</strong><br/>
        <a href="${BioModels.BM_ROOT_URL}" style="color:#0F5CB1;">${BioModels.BM_ROOT_URL}</a>
      </p>
    </div>
    <hr style="border:none;border-top:1px solid #e8e8e8;margin:0;"/>
    <div style="background-color:#f8f8f8;padding:20px 32px;font-size:12px;color:#777777;line-height:1.6;">
      You are receiving this email because you have an account on
      <a href="${BioModels.BM_ROOT_URL}" style="color:#0F5CB1;">BioModels</a>,
      a repository of mathematical models of biological processes.
      This is an automatically generated email &mdash; replies are not monitored.<br/><br/>
      BioModels is maintained by the Laboratory for Systems Medicine,
      Department of Medicine, Division of Pulmonary &ndash; Systems Medicine,
      <a href="https://systemsmedicine.pulmonary.medicine.ufl.edu/biomodels/" style="color:#0F5CB1;">University of Florida</a>.<br/>
      &copy; ${new Date().format("YYYY")} University of Florida Health
    </div>
  </div>
</div>
"""
        userService.sendEmail(USER, BODY, SUBJECT)
    }

    private void emailOTP(final String realName, final String toEmail, final String OTP) {
        final String CONTACT = grailsApplication.config.jummp.security.registration.email.contact
        final String BODY = """
<div style="background-color:#f4f4f4;margin:0;padding:32px 0;font-family:Arial,Helvetica,sans-serif;color:#333333;">
  <div style="max-width:620px;margin:0 auto;background-color:#ffffff;border-radius:4px;overflow:hidden;box-shadow:0 2px 6px rgba(0,0,0,0.10);">
    <div style="background-color:#ED6B21;height:5px;"></div>
    <div style="background-color:#072C55;padding:24px 32px 20px;">
      <div style="font-size:22px;font-weight:bold;color:#ffffff;letter-spacing:0.5px;">BioModels</div>
      <div style="font-size:12px;color:rgba(255,255,255,0.75);margin-top:4px;letter-spacing:0.3px;">Laboratory for Systems Medicine &bull; University of Florida</div>
    </div>
    <div style="padding:32px;font-size:15px;line-height:1.7;color:#333333;">
      <p style="margin:0 0 16px;">Dear ${realName},</p>
      <p style="margin:0 0 16px;">As an added layer of security to your BioModels account, please use the verification code below to complete your sign-in.</p>
      <div style="background-color:#f0f4fa;border-left:4px solid #072C55;padding:20px;margin:0 0 20px;text-align:center;border-radius:0 4px 4px 0;">
        <div style="font-size:36px;font-weight:bold;letter-spacing:8px;color:#072C55;">$OTP</div>
      </div>
      <p style="margin:0 0 16px;">This code expires in <strong>15 minutes</strong>. <strong>Do not share it with anyone.</strong></p>
      <p style="margin:0 0 16px;">If you did not request this code, please <a href="mailto:${CONTACT}" style="color:#0F5CB1;">contact us</a> immediately.</p>
      <p style="margin:0 0 16px;">Kind regards,<br/><strong>The BioModels Team</strong><br/>
        <a href="${BioModels.BM_ROOT_URL}" style="color:#0F5CB1;">${BioModels.BM_ROOT_URL}</a>
      </p>
    </div>
    <hr style="border:none;border-top:1px solid #e8e8e8;margin:0;"/>
    <div style="background-color:#f8f8f8;padding:20px 32px;font-size:12px;color:#777777;line-height:1.6;">
      You are receiving this email because you have an account on
      <a href="${BioModels.BM_ROOT_URL}" style="color:#0F5CB1;">BioModels</a>,
      a repository of mathematical models of biological processes.
      This is an automatically generated email &mdash; replies are not monitored.<br/><br/>
      BioModels is maintained by the Laboratory for Systems Medicine,
      Department of Medicine, Division of Pulmonary &ndash; Systems Medicine,
      <a href="https://systemsmedicine.pulmonary.medicine.ufl.edu/biomodels/" style="color:#0F5CB1;">University of Florida</a>.<br/>
      &copy; ${new Date().format("YYYY")} University of Florida Health
    </div>
  </div>
</div>
"""
        final String SUBJECT = "[BioModels] Your verification code"
        userService.sendEmail(toEmail, BODY, SUBJECT)
    }
}
