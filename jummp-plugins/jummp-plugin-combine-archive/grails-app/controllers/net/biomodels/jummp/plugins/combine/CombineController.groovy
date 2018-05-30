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





package net.biomodels.jummp.plugins.combine

import net.biomodels.jummp.core.annotation.QualifierTransportCommand
import net.biomodels.jummp.core.annotation.ResourceReferenceTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand

/**
 * Controller for handling Model files in the Combine Archive format.
 * @author  Raza Ali <raza.ali@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
class CombineController {
    def metadataDelegateService

    def show = {
        def model = flash.genericModel
        RevisionTransportCommand r = model.revision
        Map<QualifierTransportCommand, List<ResourceReferenceTransportCommand>> genericAnno =
            metadataDelegateService.fetchGenericAnnotations r
        if (genericAnno) {
            model["genericAnnotations"] = genericAnno
        }
        render(view: "/model/combine/show", model: model)
    }
}
