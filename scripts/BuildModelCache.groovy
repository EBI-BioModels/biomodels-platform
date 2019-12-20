/**
 * Copyright (C) 2010-2018 EMBL-European Bioinformatics Institute (EMBL-EBI),
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


import grails.plugin.springsecurity.SpringSecurityUtils
import groovyx.gpars.GParsPool
import net.biomodels.jummp.core.JummpException
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.plugins.security.UserRole
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

import java.time.Duration
import java.time.Instant

/**
 * @author Tung Nguyen <nvntung@gmail.com> on 10/12/19.
 */
class ModelCacheBuilder {
    def ctx

    private static final Logger logger = LoggerFactory.getLogger(ModelCacheBuilder.class)

    static final String adminUsername = System.getenv("ADMIN_USER")
    // auth token for admin account; used by worker threads to publish models
    static final Authentication adminAuth = createTokenForUser(adminUsername)

    /*
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
     * @see {@link Ctx#createTokenForUser(net.biomodels.jummp.plugins.security.User)}
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

    void copyRevisionFiles(Revision revision) {
        ctx.repositoryFileService.updateModelRevisionCache(revision)
    }

    void build() {
        String duration
        Instant startTime = Instant.now()
        ctx.persistenceInterceptor?.init()

        try {
            String query = "select id from Revision"
            List revisions = Revision.executeQuery(query)
            final int POOL_SIZE = 2
            GParsPool.withPool(POOL_SIZE) {
                revisions.eachParallel { long revisionId ->
                    simpleRunAs(adminAuth, {
                        ctx.persistenceInterceptor?.init()
                        Revision revision = Revision.read(revisionId)
                        try {
                            copyRevisionFiles(revision)
                        } catch (Exception e) {
                            String message = """\
Cannot copy files of the model: ${revision.model.submissionId}, revision: ${revisionId} because of the below error: 
${e}"""
                            logger.error(message)
                            e.printStackTrace()
                        } finally {
                            ctx.persistenceInterceptor?.destroy()
                        }
                    })
                }
            }
        } finally {
            ctx.persistenceInterceptor?.destroy()
            duration = Duration.between(startTime, Instant.now())
            String formattedDuration = duration.toString()
            logger.info("Initialised the model cache directory in $formattedDuration")
        }
    }
}

new ModelCacheBuilder(ctx: ctx).build()
