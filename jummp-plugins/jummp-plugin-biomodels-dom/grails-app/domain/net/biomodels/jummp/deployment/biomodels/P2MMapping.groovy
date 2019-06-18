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

import net.biomodels.jummp.model.Model

/**
 * Domain class for mapping a representative model with other models in the same genus
 *
 * These models were auto-generated in Path2Models project. Because of its complexity, we cluster
 * them into smaller groups based on genus. A cluster should have a representative model which
 * is chosen from the list of models in the same genus. Without losing generality, the first model
 * in the alphabetical order is nominated.
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
class P2MMapping implements Serializable {
    private static final long serialVersionUID = 1L

    Model representative
    String member

    static mapping = {
        id composite: ['representative', 'member']
        version false
    }
}



