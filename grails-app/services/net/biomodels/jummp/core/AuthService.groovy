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
import net.biomodels.jummp.core.constants.BioModels
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.security.IAuthService
import net.biomodels.jummp.security.TwoFactorAuth
import net.biomodels.jummp.security.TwoFactorAuth as TFA
import net.biomodels.jummp.utils.MathUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Transactional
class AuthService implements IAuthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class)
    def grailsApplication
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
    String doGenerateOTP(final String username, final String remoteAddress, final String sessionId) {
        final User USER = userService?.currentUser
        TFA auth = null
        String queryString = "select id from TwoFactorAuth t where t.user.id = :userId and t.sessionId = :sessionId"
        List results = TFA.executeQuery(queryString, [userId: USER.id, sessionId: sessionId])
        if (!results.isEmpty()) {
            auth = TFA.get(results.first())
        }
        String msg
        if (auth) {
            boolean valid
            use(groovy.time.TimeCategory) {
                def duration = new Date() - auth.issuedDate
                // valid if the issued date is not over 15 minutes
                valid = duration.minutes*60 + duration.seconds < 15*60
            }
            if (valid) {
                msg = "Reused the OTP ${auth.otp} for username: $username; address: $remoteAddress; session: $sessionId"
                println(msg)
                LOGGER.info(msg)
                emailOTP(USER, auth.otp)
                return auth.otp
            }
        }
        String otp = MathUtils.generatePassword('0123456789', 6)
        msg = "Created a new OTP $otp for username: $username; address: $remoteAddress; session: $sessionId"
        println(msg)
        LOGGER.info(msg)
        auth = new TFA(user: USER, sessionId: sessionId, otp: otp, issuedDate: new Date())
        if (!auth.save(flush: true)) {
            LOGGER.error("Cannot create a new OTP requested by user $username (sessionId: $sessionId).")
            return ""
        }
        LOGGER.info("A newly issued OTP: $otp for the user $username")

        otp
    }

    @Override
    boolean doVerifyOTP(final String username, final String otp, final String sessionId) {
        String msg = "Verifying the OTP $otp for the user $username at the session $sessionId"
        LOGGER.info(msg)
        List<TFA> results = findAll(username, otp, sessionId)
        if (results.isEmpty()) {
            msg = "Cannot find any match for OTP $otp provided by the user $username at the ssession $sessionId"
            LOGGER.debug(msg)
            return false
        }
        TFA first = results?.first()
        if (first) {
            boolean valid
            use(groovy.time.TimeCategory) {
                def duration = new Date() - first.issuedDate
                // println "Days: ${duration.days}, Hours: ${duration.hours}, etc."
                // valid if the issued date is not over 15 minutes
                valid = duration.minutes*60 + duration.seconds < 15*60
            }
            if (valid) {
                // make it expired because it has already been used. Should we?
            }
            return valid
        } else {
            msg = "Cannot find any match for OTP $otp provided by the user $username at the ssession $sessionId"
            LOGGER.debug(msg)
            return false
        }
    }

    private void emailOTP(final User USER, final String OTP) {
        final String SENDER = grailsApplication.config.jummp.security.registration.email.sender
        final String BODY = """\
<div style="background-color: lightgrey; width: 500px; border: 3px solid green; padding: 20px; margin: auto">\
<p>Hi ${USER.person.userRealName},</p>\
<h3>You're nearly there!</h3>\
<p>As an added layer of security to your account in BioModels, please use the code below to verify your \
identity.</p>\
<h2 style="background-color: grey; text-align: center; font-weight: bold; padding: 20px 0px 20px">$OTP</h2>\
<p>This code expires in 15 minutes. <b>Don't share it with anyone.</b></p>\
<p>If you think you didn't request this code, please <a href="mailto:${SENDER}">contact us</a>.</p>\
<p>Thank you for helping us keep your account secure.</p>\
<p>Kind regards,<br/><em>The BioModels Team</em></p>\
<div>\
<hr/>\
<p style="font-size: smaller">This is an automatically generated email. \
Replies to this email address aren't monitored.<br/>\
&copy; ${new Date().format("YYYY")} <a href="${BioModels.BM_ROOT_URL}" target="_blank">BioModels</a>, \
<a href="https://www.ebi.ac.uk/about/teams/molecular-networks/" target="_blank">Molecular Networks Team</a>, \
<a href="https://www.ebi.ac.uk" target="_blank">EMBL-EBI</a>, 
Wellcome Genome Campus, Hinxton, \
Cambridgeshire, CB10 1SD, UK. +44 (0)1223 49 44 44.</p>
"""
        final String SUBJECT = "[BioModels] $OTP is your verification code"
        userService.sendEmail(USER, BODY, SUBJECT)
    }
}
