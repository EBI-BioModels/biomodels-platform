/**
 * Copyright (C) 2010-2022 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

package net.biomodels.jummp.core

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class UpdateCachedParametersOnRedisJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(this.getClass())

    def parameterSearchService

    static triggers = {
        // execute job once in 10 seconds for development;
        // has to be set an appropriate repeat interval later for testing
        //simple name: "updateParametersCachedOnRedis", startDelay: 10000, repeatInterval: 7_200_000L

        // the job is run at 02:00 P.M. on Wednesday every week
        cron name: "updateParametersCachedOnRedis", cronExpression: "0 0 10 ? * TUE"
    }

    def execute() {
        String msgLog = """\
QuartzJob: Started updating the cached parameters on Redis."""
        LOGGER.info(msgLog)
        println(msgLog)
        parameterSearchService.updateRedisCache()
        msgLog = "QuartzJob: Completed updating the cached parameters on Redis."
        LOGGER.info(msgLog)
        println(msgLog)
    }
}
