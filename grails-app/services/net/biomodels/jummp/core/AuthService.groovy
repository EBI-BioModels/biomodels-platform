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
import net.biomodels.jummp.utils.MathUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Transactional
class AuthService implements IAuthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class)
    def grailsApplication
    def userService

    def doGenerateOTP(final String username, final String remoteAddress, final String sessionId) {
        println "Created a new OTP for username: $username; address: $remoteAddress; session: $sessionId"
        LOGGER.info("Created a new OTP for username: $username; address: $remoteAddress; session: $sessionId")
        String otp = MathUtils.generatePassword('0123456789', 6)
        println "A newly issued OTP: $otp"
        LOGGER.info("A newly issued OTP: $otp for the user $username")
        final String SENDER = grailsApplication.config.jummp.security.registration.email.sender
        final User USER = userService?.currentUser
        final String BODY = """\
<div style="background-color: lightgrey; width: 500px; border: 3px solid green; padding: 20px; margin: 20px">\
<p>Hi $username,</p>\
<h3>You're nearly there!</h3>\
<p>As an added layer of security to your account in BioModels, please use the code below to verify your \
identity.</p>\
<h2 style="background-color: grey; text-align: center; font-weight: bold; padding: 20px 0px 20px">$otp</h2>\
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
        final String SUBJECT = "[BioModels] $otp is your verification code"
        userService.sendEmail(USER, BODY, SUBJECT)
    }
}
