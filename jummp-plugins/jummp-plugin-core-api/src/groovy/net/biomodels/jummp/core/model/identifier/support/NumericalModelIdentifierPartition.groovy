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

package net.biomodels.jummp.core.model.identifier.support

/**
 * @short Responsible for storing information about the numerical part of a model identifier.
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
class NumericalModelIdentifierPartition extends ModelIdentifierPartition {
    /* Indicates whether a numerical part of the model identifier is constant or not. */
    boolean fixed

    NumericalModelIdentifierPartition(String fixedSetting, String widthSetting) {
        fixed = Boolean.parseBoolean(fixedSetting)
        width = Integer.parseInt(Objects.requireNonNull(widthSetting,
            'Numerical partition width required'))
        value = "0".padLeft(width, '0')
    }

    @Override
    boolean validate() {
        return fixed != null && super.validate()
    }
}
