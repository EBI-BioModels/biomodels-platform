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

/**
 * Abstract class for representing mapping of representative models and its members.
 *
 * This class has been designed to use in declaration of generic variables in ModelFilters.
 * We leave P2MMapping table intact and create a corresponding table for Uhlen's models.
 * As the structures of the tables are most identical, the abstract is essential to provide
 * some common functionality and to be used in variable declaration.
 *
 * We might have another large scale submissions in the future. If you have to implement a new
 * mapping domain class, it should be extended this abstract class. Please see UhlenModelMapping
 * and P2MMapping class for your reference.
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
abstract class AutoGenModelMapping {
    // The representative model identifier
    String representative

    // The identifier of the others in the same group
    String member
}
