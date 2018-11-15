/*
 * Copyright (C) 2010-2018 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

package net.biomodels.jummp.importer.support.biomodels

/**
 * This class is used to check whether the model in question was recently modified or not.
 * The detector bases on the latest modified date.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 *
 * Created by Mihai Glonț on 08/02/18.
 * Updated by Tung nguyen on 04/06/18.
 */
class ModelLastModifiedChangeDetector extends AbstractModelChangeDetector {
    @Override
    boolean hasChanged(ModelComparisonContext comparison) {
        Objects.requireNonNull(comparison)
        Objects.requireNonNull(comparison.imported)
        if (comparison.imported.uploadDate != comparison.lastModified) {
            return true
        }
        super.hasChanged(comparison)
    }
}
