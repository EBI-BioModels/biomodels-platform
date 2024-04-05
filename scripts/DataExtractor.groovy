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
 */

import groovyx.gpars.GParsPool
import net.biomodels.jummp.core.model.ModelState
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.utils.RunScriptHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.time.Duration
import java.time.Instant

/**
 * @author Tung Nguyen <nvntung@gmail.com> on 20/09/2023
 */
class DataExtractor extends RunScriptHelper {
    def ctx
    private static final Logger logger = LoggerFactory.getLogger(DataExtractor.class)

    void lookForSedMlFileInModel(final String submissionId) {
        List files = ctx.modelDelegateService.retrieveModelFiles(submissionId)
        boolean isSedMl = files*.filename.find { name ->
            String ext = name[(name.lastIndexOf('.') + 1)..(name.length() - 1)]
            ext == 'sedml'
        }
        if (isSedMl) {
            println submissionId
        }
    }

    /**
     * Gets all SBML based models which one of the additional files is in SEDML format
     */
    @Override
    void run() {
        String duration
        Instant startTime = Instant.now()
        ctx.persistenceInterceptor?.init()

        try {
            String query = """\
SELECT distinct m.id, m.submissionId
FROM Revision AS r JOIN r.model AS m
WHERE
    r.deleted = false
    AND m.deleted = false
    AND r.state = '${ModelState.PUBLISHED}'
	AND m.submissionId NOT LIKE 'BMID%'
	AND r.format.identifier = 'SBML'
ORDER BY m.id desc
"""
            Map namedParams = [:]
            Map metaParams = [:]
            List models = Revision.executeQuery(query, namedParams, metaParams)
            final int POOL_SIZE = 4
            String submissionId
            GParsPool.withPool(POOL_SIZE) {
                models.eachParallel { def m ->
                    simpleRunAs(adminAuth, {
                        ctx.persistenceInterceptor?.init()
                        submissionId = m[1]
                        try {
                            logger.info("Processing the model ${m[1]}...")
                            println("Processing the model ${m[1]}...")
                            lookForSedMlFileInModel(m[1])
                        } catch (Exception e) {
                            String message = """\
Cannot retrieve the files of model ${submissionId} because of the below error:\n${e}"""
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
            logger.info("Task has been completed in $formattedDuration seconds")
        }
    }
}

new DataExtractor(ctx: ctx).run()
