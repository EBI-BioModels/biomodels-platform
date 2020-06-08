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
 * Grants writable permission for fellow curators on models submitted by other curators
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
'ROLE_CURATOR') and R.revisionNumber = 1 group by M.id"""
        List models = Model.executeQuery(query)
        models
    }

    void grantWritablePermission(final Authentication adminAuth, final Model model) {
        RunScriptHelper.simpleRunAs(adminAuth, {
            println("""Granting writable permission for ROLE_CURATOR on the model ${model.submissionId}""")
            // similar to ctx.modelService.submitModelRevisionForPublication(revision)
            // via the model directly
            // grant permissions to ROLE_CURATOR
            if (!ctx.modelService.hasPermission(model, "ROLE_CURATOR", BasePermission.READ)) {
                ctx.aclUtilService.addPermission(model, "ROLE_CURATOR", BasePermission.READ)
            }
            if (!ctx.modelService.hasPermission(model, "ROLE_CURATOR", BasePermission.WRITE)) {
                ctx.aclUtilService.addPermission(model, "ROLE_CURATOR", BasePermission.WRITE)
            }
            if (!ctx.modelService.hasPermission(model, "ROLE_CURATOR", BasePermission.ADMINISTRATION)) {
                ctx.aclUtilService.addPermission(model, "ROLE_CURATOR", BasePermission.ADMINISTRATION)
            }

            model.revisions.each { Revision rev ->
                if (!ctx.modelService.hasPermission(rev, "ROLE_CURATOR", BasePermission.READ)) {
                    ctx.aclUtilService.addPermission(rev, "ROLE_CURATOR", BasePermission.READ)
                }
                if (!ctx.modelService.hasPermission(rev, "ROLE_CURATOR", BasePermission.ADMINISTRATION)) {
                    ctx.aclUtilService.addPermission(rev, "ROLE_CURATOR", BasePermission.ADMINISTRATION)
                }
            }
        })
    }

    void runJob() {
        println "Started the job: ${new Date().format("dd/MM/yyyy HH:mm:ss")}"
        String duration
        Instant startTime = Instant.now()
        ctx.persistenceInterceptor?.init()
        try {
            List revisions = getAllModelsSubmittedByCurators()
            println "Nb. models: ${revisions?.size()}"
            final int POOL_SIZE = 2
            GParsPool.withPool(POOL_SIZE) {
                revisions.eachParallel { entry ->
                    RunScriptHelper.simpleRunAs(adminAuth, {
                        ctx.persistenceInterceptor?.init()
                        Model model = Model.get(entry[0])
                        try {
                            grantWritablePermission(adminAuth, model)
                        } catch (Exception e) {
                            String message = """Cannot fix writable permission on the revision: ${entry[2]} and
admin rights on the model: ${model.submissionId} for ROLE_CURATOR because of the below error: ${e}"""
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
            logger.debug("Fixed permission in $formattedDuration")
        }
        println "Completed the job: ${new Date().format("dd/MM/yyyy HH:mm:ss")}"
    }
}

new ModelPermissionRepairer(ctx: ctx).runJob()
