/**
* Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
* Spring Security (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Spring Security used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.core

import org.springframework.security.core.Authentication
import net.biomodels.jummp.core.user.AuthenticationHashNotFoundException
import org.springframework.security.authentication.AnonymousAuthenticationToken as AAT
import org.springframework.security.core.authority.GrantedAuthorityImpl

/**
 * @short Service implementing the IAuthenticationHashService
 *
 * @author Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 */
class AuthenticationHashService implements IAuthenticationHashService {
    /**
     * Dependency Injection of grailsApplication
     */
    @SuppressWarnings("GrailsStatelessService")
    def grailsApplication
    /**
     * Private class describing a hash entry in the authentication Map.
     */
    private class AuthenticationHashEntry {
        /**
         * Last access to this entry.
         */
        Date timeStamp
        /**
         * The Authentication represented by this entry.
         */
        Authentication authentication
        AuthenticationHashEntry(Date timeStamp, Authentication auth) {
            this.timeStamp = timeStamp
            this.authentication = auth
        }
    }
    private static final ANONYMOUS_AUTH = new AAT("key", "anonymousUser",
            [new GrantedAuthorityImpl("ROLE_ANONYMOUS")])

    static transactional = true
    /**
     * Random number generator for creating the hash values.
     */
    private final Random random = new Random(System.currentTimeMillis())
    /**
     * Map of the Authentications and their Ids.
     */
    @SuppressWarnings('GrailsStatelessService')
    private Map<String, AuthenticationHashEntry> authentications = [:]

    String hashAuthentication(Authentication auth) {
        String hash = random.nextInt() + auth.name + System.currentTimeMillis()
        hash = hash.encodeAsMD5()
        authentications.put(hash, new AuthenticationHashEntry(new Date(), auth))
        return hash
    }

    Authentication retrieveAuthentication(String hash) throws AuthenticationHashNotFoundException {
        if (hash == "anonymous") {
            return ANONYMOUS_AUTH
        }
        if (authentications.containsKey(hash)) {
            AuthenticationHashEntry entry = authentications[hash]
            entry.timeStamp = new Date()
            return entry.authentication
        } else {
            throw new AuthenticationHashNotFoundException("No Authentication for Hash " + hash)
        }
    }

    void checkAuthenticationExpired() {
        long time = Long.valueOf(grailsApplication.config.jummp.authenticationHash.maxInactiveTime)
        long maxInactiveTime = new Date().getTime() - time
        List authenticationsTmp = []
        authentications.each { user, hash ->
            long timeStamp = hash.timeStamp.getTime()
            if (timeStamp < maxInactiveTime) {
                authenticationsTmp.add(user)
            }
        }
        authenticationsTmp.each { user ->
            authentications.remove(user)
        }
    }

    boolean isAuthenticated(String hash) {
        return authentications.containsKey(hash)
    }
}
