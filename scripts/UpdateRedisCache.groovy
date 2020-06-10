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

import net.biomodels.jummp.utils.RunScriptHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication

import java.time.Duration
import java.time.Instant

/**
 *
 * @author <a href="mailto:nvntung@gmail.com">Tung Nguyen</a> on 04/06/20.
 */
class RedisCacheUpdater {
    def ctx

    private static final Logger logger = LoggerFactory.getLogger(RedisCacheUpdater.class)

    static final String adminUsername = System.getenv("ADMIN_USER")
    // auth token for admin account; used by worker threads to publish models
    static final Authentication adminAuth = RunScriptHelper.createTokenForUser(adminUsername)

    void update() {
        String duration
        Instant startTime = Instant.now()
        ctx.persistenceInterceptor?.init()

        try {
            RunScriptHelper.simpleRunAs(adminAuth, {
                ctx.persistenceInterceptor?.init()
                try {
                    ctx.decorationService.updateDataForWidgetsOnHomePage()
                } catch (Exception e) {
                    String message = "Cannot update Redis Cache because of the below error: ${e}"
                    logger.error(message)
                    e.printStackTrace()
                } finally {
                    ctx.persistenceInterceptor?.destroy()
                }
            })
        } finally {
            ctx.persistenceInterceptor?.destroy()
            duration = Duration.between(startTime, Instant.now())
            String formattedDuration = duration.toString()
            logger.debug("Updated the Redis Cache in $formattedDuration")
        }
    }
}

new RedisCacheUpdater(ctx: ctx).update()
