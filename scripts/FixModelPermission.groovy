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

import groovyx.gpars.GParsPool
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.utils.RunScriptHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.core.Authentication

import java.time.Duration
import java.time.Instant

/**
 * Grants writeable permission for fellow curators on models submitted by other curators
 * @author <a href="mailto:nvntung@gmail.com">Tung Nguyen</a> on 04/06/20.
 */
class ModelPermissionRepairer {
    def ctx

    private static final Logger logger = LoggerFactory.getLogger(ModelPermissionRepairer.class)

    static final String adminUsername = System.getenv("ADMIN_USER")
    // auth token for admin account; used by worker threads to publish models
    static final Authentication adminAuth = RunScriptHelper.createTokenForUser(adminUsername)

    List getAllModelsSubmittedByCurators() {
        String query = """select M.id, M.submissionId, R.id, R.owner.id
from Revision As R JOIN R.model As M
where R.owner.id IN (select U.id from UserRole AS UR JOIN UR.role AS RO JOIN UR.user AS U where RO.authority =
'ROLE_CURATOR') group by M.id"""
        List models = Model.executeQuery(query)
        models
    }

    List getAllCurators() {
        String query = """select U.id, U.username from UserRole AS UR JOIN UR.role as Ro JOIN UR.user AS U
where Ro.authority = 'ROLE_CURATOR'
"""
        List curators = Model.executeQuery(query)
        curators
    }

    void grantWritablePermission(Authentication authentication, Model model, Revision revision) {
        final Authentication modelOwnerAuth = RunScriptHelper.createTokenForUser(revision.owner.username)
        RunScriptHelper.simpleRunAs(modelOwnerAuth, {
            ctx.modelService.submitModelRevisionForPublication(revision)
        })
    }

    void fixPermission(Authentication authentication, Model model, Revision revision) {
        boolean canUpdate = false
        if (model.deleted) {
            canUpdate = false
        } else {
            canUpdate = ctx.aclUtilService.hasPermission(authentication, model, BasePermission.WRITE)
            if (!canUpdate) {
                println("""Curator ${authentication.principal} has no write permission on the model ${model
                    .submissionId}.${revision.revisionNumber}""")
                if (model.submissionId == 'MODEL1804030001') { //MODEL1805010003
                    println("""Granting writable permission for curator ${authentication.principal} on the model
${model.submissionId}.${revision.revisionNumber}""")
                    grantWritablePermission(authentication, model, revision)
                }
            }
        }
    }

    void runJob() {
        println "Started the job: ${new Date().format("dd/MM/yyyy HH:mm:ss")}"
        String duration
        Instant startTime = Instant.now()
        ctx.persistenceInterceptor?.init()
        try {
            List revisions = getAllModelsSubmittedByCurators()
            List curators = getAllCurators()
            final int POOL_SIZE = 2
            GParsPool.withPool(POOL_SIZE) {
                curators.each { u ->
                    long userID = u[0] as long
                    String username = u[1] as String
                    Authentication authentication = RunScriptHelper.createTokenForUser(username)
                    revisions.eachParallel { entry ->
                        long modelOwnerID = entry[3] as long
                        if (userID != modelOwnerID) {
                            RunScriptHelper.simpleRunAs(authentication, {
                                ctx.persistenceInterceptor?.init()
                                Model model = Model.get(entry[0])
                                Revision revision = Revision.get(entry[2])
                                try {
                                    fixPermission(authentication, model, revision)
                                } catch (Exception e) {
                                    String message = """Cannot fix permission on the model: ${model.submissionId}, revision: ${entry[2]} for user ${username} because of the below error:${e}"""
                                    logger.error(message)
                                    e.printStackTrace()
                                } finally {
                                    ctx.persistenceInterceptor?.destroy()
                                }
                            })
                        }
                    }
                }
            }
        } finally {
            ctx.persistenceInterceptor?.destroy()
            duration = Duration.between(startTime, Instant.now())
            String formattedDuration = duration.toString()
            logger.debug("Fixed permission in $formattedDuration")
        }
        println "Completed the job: ${new Date().format("dd/MM/yyyy HH:mm:ss")}"
    }
}

new ModelPermissionRepairer(ctx: ctx).runJob()
