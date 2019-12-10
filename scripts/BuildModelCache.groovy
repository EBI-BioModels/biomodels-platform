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
import net.biomodels.jummp.core.vcs.*
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.plugins.security.UserRole
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.time.Instant

/**
 * @author Tung Nguyen <nvntung@gmail.com> on 10/12/19.
 */
class AppCtx {

    static final String adminUsername = System.getenv("ADMIN_USER")
    // auth token for admin account; used by worker threads to publish models
    static final Authentication adminAuth = createTokenForUser(adminUsername)

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
}

class ModelCacheBuilder {
    def ctx

    void copyRevisionFiles(Revision revision) throws RuntimeException {
        String modelId = revision.model.submissionId
        String revNum = revision.revisionNumber.toString()
        println "Copying the files associated with the revision ${revision.vcsId} (${revision.id}): ${modelId}.${revNum}"
        try {
            File modelDirectory = new File(ctx.repositoryFileService.MODEL_CACHE_DIR, revision.model.submissionId)
            if (!modelDirectory.exists()) {
                modelDirectory.mkdirs()
            }
            File modelRevDir = new File(modelDirectory, revision.revisionNumber.toString())
            if (!modelRevDir.exists()) {
                modelRevDir.mkdirs()
            }
            try {
                List<File> files = ctx.vcsService.retrieveFiles(revision)
                files.each {
                    println "File: ${it.absolutePath}"
                    Path path = Files.copy(it.toPath(),
                        new File(modelRevDir, it.getName()).toPath(),
                        StandardCopyOption.REPLACE_EXISTING)
                    println "Copied: ${path.toString()}"

                }
            } catch (VcsException | InvalidVcsRepositoryException | VcsAlreadyInitedException | VcsNotInitedException |
            FileAlreadyVersionedException | FileNotVersionedException e) {
                println "Encountered VCS errors for model ${modelId}, revision number ${revNum} (${revision.vcsId})"
            }
        } catch (RuntimeException e) {
            println "Encountered the problem: ${e.toString()}"
        }
    }

    void build() {
        String duration
        Instant startTime = Instant.now()
        try {
            AppCtx.simpleRunAs(AppCtx.adminAuth, {
                // iterate on the list of revisions
                Revision.withTransaction {
                    List revisions = Revision.getAll()
                    revisions.eachWithIndex{ Revision revision, int i ->
                        copyRevisionFiles(revision)
                    }
                }
            })
        } catch (Exception e) {
            System.err.println("Generic exception encountered while initialising model cache directory: $e")
        } finally {
            duration = Duration.between(startTime, Instant.now())
            String formattedDuration = duration.toString()
            println "Initialised the model cache directory in $formattedDuration"
        }
    }
}

new ModelCacheBuilder(ctx: ctx).build()
