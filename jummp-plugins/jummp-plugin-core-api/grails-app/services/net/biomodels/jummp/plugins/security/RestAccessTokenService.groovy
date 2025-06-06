/**
 * Copyright (C) 2010-2024 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
 * Lucene, Apache Commons, Perf4j, Spring Security (or a modified version of that library), containing parts
 * covered by the terms of Apache License v2.0, the licensors of this
 * Program grant you additional permission to convey the resulting work.
 * {Corresponding Source for a non-source form of such a combination shall
 * include the source code for the parts of Lucene, Apache Commons, Perf4j, Spring Security used as well as
 * that of the covered work.}
 */

package net.biomodels.jummp.plugins.security

import grails.plugin.springsecurity.rest.RestTokenCreationEvent
import grails.plugin.springsecurity.userdetails.GrailsUser
import grails.transaction.Transactional
import groovy.time.TimeCategory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener

/**
 * This service manages access tokens for REST API authentication.
 */
@Transactional
class RestAccessTokenService implements ApplicationListener<RestTokenCreationEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestAccessTokenService.class)

    def redisService
    def userService

    @Override
    void onApplicationEvent(RestTokenCreationEvent event) {
        if (!event) {
            throw new IllegalStateException("""Cannot determine credential and principal. \
The request to issue an access token was failed.""")
        }
        GrailsUser user = event.principal as GrailsUser
        String username = user?.username
        User requester = userService.getUser(username)
        String newToken = event?.accessToken // the newly issued token
        // 1. Look for and delete all the tokens issued in the former requests
        def qStr = "select id from AuthToken as AT where AT.username = :username and token != :token"
        List<Long> ids = AuthToken.executeQuery(qStr, [username: username, token: newToken]) as List<Long>
        if (ids) {
            ids.each { Long i ->
                AuthToken.where {
                    id == i
                }.deleteAll()
            }
            LOGGER.info("Tokens $ids of the user: $username will be deleted now.")
        }
        // 2. Set an expiry date for the newly issued token
        AuthToken authToken = AuthToken.findByTokenAndUsername(newToken, username)
        if (authToken) {
            AuthTokenManager tokenManager = new AuthTokenManager(hitCount: 0, user: requester)
            tokenManager.accessToken = newToken
            tokenManager.createdDate = new Date()
            tokenManager.expiredDate = new Date() + 30
            if (!tokenManager.save(flush: true)) {
                LOGGER.debug("""Cannot create the details for the token ([id: $authToken.id]) \
due to ${tokenManager.getErrors().toString()}.""")
            }
        }

        // 3. Send an email to the requester/user and say that your access token
        // will be expired after 30 days of usage, for example.
        try {
            confirmByEmail(requester, username, newToken[-8..-1])
        } catch (Exception e) {
            LOGGER.error("""Failed to send the confirmation to the user \
$username (${requester.person.userRealName}) when issuing a new access token. The cause is ${e.message}.""")
            e.printStackTrace()
        }
    }

    Map<Long, Object> doCheckAndExpireAccessTokens() {
        List<AuthToken> allTokens = AuthToken.getAll()
        Map result = [:]
        for (AuthToken token : allTokens) {
            Map m = expireAccessToken(token)
            result.put(token.id, m)
        }
        // the block below aimed for debugging
        /*
        result.each { def key, def value ->
            LOGGER.info("Token ${key}: ${value}")
        }*/
        result as Map<Long, Object>
    }

    Map<String, Object> expireAccessToken(final AuthToken authToken) {
        doExpireAccessToken(authToken)
    }

    Map<String, Object> expireAccessToken(final String token, final String username) {
        // Look up the access token using two params: token and username
        AuthToken authToken = AuthToken.findByTokenAndUsername(token, username)
        doExpireAccessToken(authToken)
    }

    private Map<String, Object> doExpireAccessToken(final AuthToken authToken) {
        if (authToken) {
            final String token = authToken.token
            final String username = authToken.username
            return doExpireAccessToken(token, username)
        } else {
            return [success: false,
                    massage: "Cannot find any access token matching with the username given"] as Map<String, Object>
        }
    }

    private Map<String, Object> doExpireAccessToken(final String accessToken, final String username) {
        //  i) check whether its associated expiredDate (in AuthTokenManager) is later than the current date or not
        // ii) if it is the case, delete that access token
        AuthTokenManager account = AuthTokenManager.findByAccessToken(accessToken)
        boolean success
        String message
        String endingToken = accessToken[-8..-1]
        if (account) {
            boolean expired = account.expiredDate < new Date()
            if (expired) {
                AuthToken.where {
                    token == accessToken
                }.deleteAll()
                notifyByEmail(account)
            } else {
                sendReminderEmail(account)
            }

            if (AuthToken.findByToken(accessToken) && expired) {
                success = false
                message = "Failed to remove/expire the token ending $endingToken of the username $username"
            } else if (AuthToken.findByToken(accessToken) && !expired) {
                success = false
                message = "The token ending $endingToken of the username $username is still valid to use."
            } else {
                success = true
                message = "Expired the token eding $endingToken of the username $username successfully"
            }
        } else {
            success = false
            message = "No access token details found for the token ending $endingToken and the username $username"
        }
        return [success: success, message: message] as Map<String, Object>
    }

    private void notifyByEmail(final AuthTokenManager atm) {
        String friendlyName = atm.user?.person?.userRealName ?: atm.user.username
        String endingToken = atm.accessToken[-8..-1]
        final String BODY = """Dear ${friendlyName},\
<p>We are writing to inform you that your access token ending <strong>$endingToken</strong> has \
expired at <strong>${atm.expiredDate.format('HH:mm:ss')}</strong> on <strong>${atm.expiredDate.format('dd-MM-yyyy')}</strong>.</p>\
<p>You can create a new access token now to avoid any unexpected downtime.</p>
<p>If you need any assistance, please contact us asap.</p>
<br/>
<p>Best regards,<br/>
The BioModels Team</p>"""
        final String SUBJECT = "[BioModels] Access Token Expiration"
        userService.sendEmail(atm.user, BODY, SUBJECT)
    }

    private void remindByEmail(final AuthTokenManager atm) {
        String friendlyName = atm.user?.person?.userRealName ?: atm.user.username
        String endingToken = atm.accessToken[-8..-1]
        final String BODY = """Dear ${friendlyName},\
<p>We are writing to inform you that your access token ending <strong>$endingToken</strong> is \
about to expire at <strong>${atm.expiredDate.format('HH:mm:ss')}</strong> on <strong>${atm.expiredDate.format('dd-MM-yyyy')}</strong>.</p>\
<p>You can create a new access token now to avoid unnecessary downtime.</p>
<p>If you need any assistance, please contact us asap.</p>
<br/>
<p>Best regards,<br/>
The BioModels Team</p>"""
        final String SUBJECT = "[BioModels] Access Token Expiring Soon"
        userService.sendEmail(atm.user, BODY, SUBJECT)
    }

    private void confirmByEmail(final User requester, final String username, final String endingToken) {
        String friendlyName = requester?.person?.userRealName ?: username
        final String BODY = """Hey ${friendlyName},<p>An access token ending <strong>$endingToken</strong> was recently issued to your account. \
The token will be expired after 30 days since now.</p>\
<p>Notes that the former tokens have been deleted, therefore, you have to update it in your work \
to avoid unnecessary interuptions.</p>
<p>If you didn't request it or you run into problems, please contact us asap.</p>
<br/>
Thank you,<br/>
The BioModels Team"""
        final String SUBJECT = "[BioModels] An access token has been issued to your account"
        userService.sendEmail(requester, BODY, SUBJECT)
    }

    private void sendReminderEmail(final AuthTokenManager account) {
        /**
         * the scheduled emails relying on the number of days left (called nbDaysLeft) which the token is valid
         * 1 < nbDaysLeft <= 7: a week left -> sending a reminder email if it hasn't been sent yet
         * 1 = nbDaysLeft: 1 day left -> sending one more reminder email if it hasn't been sent yet
         */
        use(TimeCategory) {
            def duration = account.expiredDate - account.createdDate
            boolean sent7daysYet = isSentReminderYet(account.user.username, "sent7daysYet")
            boolean sent1dayYet = isSentReminderYet(account.user.username, "sent1dayYet")
            boolean shouldSendReminderEmail =
                (duration.days <= 7 && duration.days > 1 && !sent7daysYet) ||
                    (duration.days == 1 && !sent1dayYet)
            if (shouldSendReminderEmail) {
                remindByEmail(account)
                if (duration.days <= 7 && duration.days > 1 && !sent7daysYet) {
                    redisService.doRedisSAdd("userLog:${account.user.username}", "sent7daysYet:true")
                } else if (duration.days == 1 && !sent1dayYet) {
                    redisService.doRedisSAdd("userLog:${account.user.username}", "sent1dayYet:true")
                }
            }
        }
    }

    boolean isSentReminderYet(final String username, final String key) {
        Set<String> uLog = redisService.doRedisSMembers("userLog:$username") as Set<String>
        String r = uLog.find { it.startsWith(key) }
        if (r) {
            return r.split(":")[1] as boolean
        } else {
            return false
        }
    }
}
