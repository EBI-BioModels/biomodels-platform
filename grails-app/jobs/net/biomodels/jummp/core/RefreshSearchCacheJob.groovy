/**
 * Copyright (C) 2010-2026 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
 * @short Refreshes the per-domain *:* Redis search cache daily.
 *
 * Warms the search-result:all:<domain> Redis keys for each EBI Search domain
 * (biomodels, biomodels_autogen, biomodels_all) so that the first users of the
 * day get fast search results without triggering an expensive EBI Search call.
 */
class RefreshSearchCacheJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshSearchCacheJob.class)

    def searchService

    static triggers = {
        // the job is run at 10:00 AM every day
        cron name: "refreshSearchCache", cronExpression: "0 0 10 * * ?"
    }

    def execute() {
        LOGGER.info("QuartzJob: Started refreshing per-domain *:* search cache")
        println("QuartzJob: Started refreshing per-domain *:* search cache")
        searchService.refreshSearchAllCache()
        LOGGER.info("QuartzJob: Finished refreshing per-domain *:* search cache")
        println("QuartzJob: Finished refreshing per-domain *:* search cache")
    }
}
