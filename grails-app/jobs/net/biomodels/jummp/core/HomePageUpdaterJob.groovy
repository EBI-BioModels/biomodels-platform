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

package net.biomodels.jummp.core

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @short Refreshes data on Redis Server
 *
 * <p>The job is run every 2 hours or started at 2AM on a daily basis
 *
 * @author <a href="mailto:tung.nguyen@ebi.ac.uk">Tung Nguyen</a>
 */
class HomePageUpdaterJob {
    private static final Logger logger = LoggerFactory.getLogger(HomePageUpdaterJob.class)
    def decorationService

    static triggers = {
        // execute job once in 10 seconds for development;
        // has to be set an appropriate repeat interval later
        // simple name: "refreshDataOnRedisServer", startDelay: 10000, repeatInterval: 7_200_000L

        // the job is run at 02:00 AM every day
        cron name: "refreshDataOnRedisServer", cronExpression: "0 0 2 * * ?"
    }

    def execute() {
        // execute job
        logger.info("""\
QuartzJob: Refreshing data for widgets shown on the home page on Redis Server at ${new Date().toString()}""")
        decorationService.updateDataForWidgetsOnHomePage()
    }
}
