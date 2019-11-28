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

package net.biomodels.jummp.deployment.biomodels

import grails.transaction.Transactional

/**
 * This service provides means of operating and managing the map between representative and missing models in
 * PDGSM project.
 *
 * @author Tung Nguyen <tnguyen@ebi.ac.uk>
 */
@Transactional
class UhlenModelService extends AutoGenModelService {
    List<String> findMissing() {
        // executeQuery() queries are not supported in unit tests with Grails 2.5, use criteria queries instead
        UhlenModelMapping.createCriteria().list {
            projections {
                property 'member'
            }
        }
    }

    String getRepresentativeId(String id) {
        UhlenModelMapping map = UhlenModelMapping.findByMember(id)
        if (map) {
            return map.representative
        } else {
            // the identifier in question is the model itself
            map = UhlenModelMapping.findByRepresentative(id)
            return map?.representative ?: null
        }
    }
}
