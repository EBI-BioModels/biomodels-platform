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

import grails.plugin.springsecurity.rest.token.AccessToken
import grails.plugin.springsecurity.userdetails.GrailsUser
import grails.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.security.authentication.event.AuthenticationSuccessEvent

/**
 * Service for counting the hits of using Access Token via REST API.
 *
 */
@Transactional
class RestValidationTokenService implements ApplicationListener<AuthenticationSuccessEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestValidationTokenService.class)

    RestValidationTokenService() {
        super()
    }

    @Override
    void onApplicationEvent(AuthenticationSuccessEvent successEvent) {
        GrailsUser principal = successEvent.source.principal as GrailsUser
        String username = principal.username
        String msg
        if (successEvent.source instanceof AccessToken) {
            String accessToken = successEvent.source.accessToken
            msg = """The system has authenticated the principal (username: $username, \
token ending ${accessToken[-8..-1]}) via Access Token Based Authentication."""
            AuthTokenManager auth = AuthTokenManager.findByAccessToken(accessToken)
            if (auth) {
                auth.hitCount = auth.hitCount + 1
                if (!auth.save(flush: true)) {
                    LOGGER.debug("Failed to increment the hit count for the token ending ${accessToken[-8..-1]}")
                }
            }
        } else {
            msg = """The system has authenticated the principal (username: $username) \
via Basic or Form Based Authentication."""
        }
        println(msg)
        LOGGER.info(msg)
    }
}
