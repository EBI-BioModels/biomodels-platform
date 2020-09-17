/**
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
 **/

package net.biomodels.jummp.core.model.identifier.support

/**
 * Initialiser for model identifier generators.
 *
 * Concrete implementations of this interface can be used to indicate to a ModelIdentifierGenerator
 * the last value it used so that the latter does not duplicate identifiers created previously.
 * @see {@link net.biomodels.jummp.core.model.identifier.generator.ModelIdentifierGenerator}
 * @see {@link net.biomodels.jummp.core.model.identifier.ModelIdentifierGeneratorFactoryBean}
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
interface ModelIdentifierGeneratorInitializer {
    String getLastUsedValue()

    /**
     * Returns Redis key of the last used value
     *
     * @return A {@link String} representing Redis key which is capturing the last used value
     */
    String getRedisKeyForLastUsedValue()
}
