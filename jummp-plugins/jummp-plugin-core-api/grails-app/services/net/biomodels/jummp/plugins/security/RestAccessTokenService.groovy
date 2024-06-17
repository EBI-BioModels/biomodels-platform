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
import grails.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener

/**
 * This service manages access tokens for REST API authentication.
 */
@Transactional
class RestAccessTokenService implements ApplicationListener<RestTokenCreationEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestAccessTokenService.class)
    @Override
    void onApplicationEvent(RestTokenCreationEvent event) {
        if (!event) {
            throw new IllegalStateException("""Cannot determine credential and principal. \
The request to issue an access token was failed.""")
        }
        def user = event.principal
        def username = user?.username
        def newToken = event?.accessToken // the newly issued token
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
        // 2. Send an email to the requester/user and say that your access token
        // will be expired after 30 days of usage, for example.
        // TODO: TBD soon

        // 3. Set an expiry date for the newly issued token
        // TODO: TBD soon
    }
}
