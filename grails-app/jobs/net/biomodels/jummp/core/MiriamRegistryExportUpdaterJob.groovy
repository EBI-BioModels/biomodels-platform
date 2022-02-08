/**
 * Copyright (C) 2010-2015 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
 * Simple job for updating the export of the identifiers.org registry.
 *
 * Fetches the latest version of the Registry on the last Saturday
 * of every month at 2:15AM
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
class MiriamRegistryExportUpdaterJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(MiriamRegistryExportUpdaterJob.class)
    def miriamService

    static triggers = {
      cron name: 'exportUpdateTrigger', cronExpression: "0 15 2 ? * 7L"
    }

    def execute() {
        LOGGER.info "Started refreshing identifiers.org registry export."
        miriamService.updateMiriamResources()
        LOGGER.info "Finished refreshing identifiers.org registry export."
    }
}
