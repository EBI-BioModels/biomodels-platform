/**
* Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

package net.biomodels.jummp.plugins.mdl

import net.biomodels.jummp.core.annotation.QualifierTransportCommand
import net.biomodels.jummp.core.annotation.ResourceReferenceTransportCommand

/**
 * Controller for rendering models encoded in the Model Description Language.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
class MdlController {
    /**
     * Dependency injection of mdlService
     */
    def mdlService
    def metadataDelegateService

    /**
     * Render the MDL-specific tabs.
     */
    def show() {
        def model = flash.genericModel
        Map<QualifierTransportCommand, List<ResourceReferenceTransportCommand>> genericAnno =
                metadataDelegateService.fetchGenericAnnotations(model.revision)
        if (genericAnno) {
            model["genericAnnotations"] = genericAnno
        }
        model["mdlFiles"] = mdlService.getMdlFilesFromRevision(model.revision)
        model["dataFiles"] = mdlService.getDataFilesFromRevision(model.revision)

        render(view: "/model/mdl/show", model: model)
    }
}
