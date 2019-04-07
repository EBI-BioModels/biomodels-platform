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
import org.apache.commons.lang.builder.HashCodeBuilder

/**
 * Domain class for managing associations between models and tags used in BioModels
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
class ModelTag implements Serializable {
    private static final long serialVersionUID = 1L

    Model model
    Tag tag

    static mapping = {
        id composite: ['model', 'tag']
    }

    static constraints = {
    }

    boolean equals(other) {
        if (!(other instanceof ModelTag)) {
            return false
        }
        model.equals(other.model) && tag.equals(other.tag)
    }

    int hashCode() {
        def builder = new HashCodeBuilder()
        builder.append(model)
        builder.append(tag)
        builder.toHashCode()
    }
}
