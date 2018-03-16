/**
* Copyright (C) 2010-2019 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
**/





/**
 * @short Service class for handling the external model conversion service
 *
 * This service provides the high-level API to access external model conversion service.
 * The conversion service is running on another host. It is equipped API endpoints whereby
 * this service could communicate to get various exports of the given model.
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 *
 * @date 20180316
 */

package net.biomodels.jummp.core

import net.biomodels.jummp.core.model.RevisionTransportCommand
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

class ModelConversionService {

    private static final Log log = LogFactory.getLog(ModelConversionService.class)

    protected Set<String> listOfFormatsSupportedExport() {
        // should retrieve from the external conversion service
        return ["SBML", "PharmML"]
    }

    def generateExports(RevisionTransportCommand cmd) {
        boolean formatSupportedExport = cmd.format.identifier in listOfFormatsSupportedExport()
        if (formatSupportedExport) {
            log.info("Connecting conversion service to generate exports of the model ${cmd?.model?.submissionId}")
        }
    }
}
