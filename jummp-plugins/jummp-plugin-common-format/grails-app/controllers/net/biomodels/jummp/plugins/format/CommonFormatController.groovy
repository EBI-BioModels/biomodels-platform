/**
 * Copyright (C) 2010-2021 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

package net.biomodels.jummp.plugins.format

import net.biomodels.jummp.core.annotation.QualifierTransportCommand as QualifierTC
import net.biomodels.jummp.core.annotation.ResourceReferenceTransportCommand as RRTC
import net.biomodels.jummp.core.annotation.StatementTransportCommand as STC
import net.biomodels.jummp.core.model.RevisionTransportCommand as RevisionTC

/**
 * <p>Controls the way of rendering format specific views</p>
 * <p style="font-weight: bold">Authors:</p>
 * <ul>
 *   <li>Mihai Glonț&nbsp;<a href="mailto:mihai.glont@ebi.ac.uk">mihai.glont@ebi.ac.uk</a></li>
 *   <li>Tung Nguyen&nbsp;<a href="mailto:tung.nguyen@ebi.ac.uk">tung.nguyen@ebi.ac.uk</a></li>
 *  </ul>
 */
class CommonFormatController {
    def metadataDelegateService

    def show() {
        def model = flash.genericModel
        final RevisionTC revision = model.revision as RevisionTC
        List<STC> statements = model.modelLevelAnnotations as List<STC>
        Map<QualifierTC, List<RRTC>> annotations = metadataDelegateService.fetchGenericAnnotations(statements)
        if (annotations) {
            model["genericAnnotations"] = annotations
        }
        model
    }
}
