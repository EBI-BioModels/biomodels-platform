/**
 * Copyright (C) 2010-2020 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
 */

package net.biomodels.jummp.utils

import grails.plugin.springsecurity.SpringSecurityUtils
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.plugins.security.UserRole
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

/**
 * Extends Spring Security Authentication or customises logging to ease running operations in customised scripts
 * @author <a href="mailto:tung.nguyen@ebi.ac.uk">Tung Nguyen</a>
 * @author <a href="mailto:mihai.glont@ebi.ac.uk">Mihai Glont</a>
 */
class RunScriptHelper {
    /**
     * Creates and returns an authentication token for the given user.
     * Note that the owner of the credentials is *not* authenticated until the token is put in the SecurityContext
     * (see simpleRunAs()). Also note that, by default, SecurityContexts are thread-local.
     *
     * @param username the username for the account whose credentials should be used to create the authentication token
     * @return the authentication token which, if placed in the security context of the current thread, would allow us
     *      to log in as the given user.
     */
    private static UsernamePasswordAuthenticationToken createTokenForUser(User u) {
        assert u
        def authorities = UserRole.findAllByUser(u)*.role*.authority.join(",")
        List<GrantedAuthority> roles = SpringSecurityUtils.parseAuthoritiesString authorities
        return new UsernamePasswordAuthenticationToken(u.username, u.password, roles)
    }

    /*
     * Creates and returns an authentication token for the given user.
     *
     * @param username the username for the account whose credentials should be used to create the authentication token
     * @return an authentication token for the given user
     * @see {@link createTokenForUser(net.biomodels.jummp.plugins.security.User)}
     */
    private static UsernamePasswordAuthenticationToken createTokenForUser(String username) {
        assert username : "Username required but not defined"
        createTokenForUser(User.findByUsername(username))
    }

    /**
     * Very simple means of executing an action as a different user than the one
     * that is currently authenticated.
     *
     * Performs the action defined by closure as the user defined by Authentication
     * object auth, then reverts to the original authentication.
     *
     * Suitable for lightweight work for which we need not create a separate thread.
     */
    static def simpleRunAs = { auth, closure ->
        def currentAuth
        try {
            currentAuth = SecurityContextHolder.context.authentication
            SecurityContextHolder.context.authentication = auth
            def result = closure.call()
            return result
        } finally {
            if (currentAuth) {
                SecurityContextHolder.context.authentication = currentAuth
            } else {
                SecurityContextHolder.clearContext()
            }
        }
    }
}
