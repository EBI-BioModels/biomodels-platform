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

package net.biomodels.jummp.plugins.security

/**
 * @short Domain class for representing user teams.
 *
 * Does not rely on Hibernate for managing the many-to-many relationship with User.
 * See http://www.infoq.com/presentations/GORM-Performance. Multiple times.
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
class Team implements Serializable {
    private static final long serialVersionUID = 1L
    /**
     * The name of the team.
     */
    String name
    /**
     * The description of the team.
     */
    String description
    /**
     * The user that has created this team.
     */
    User owner

    static constraints = {
        owner bindable: false //exclude this from the data binding process
        name blank: false, unique: "owner"
        description nullable: true
    }
}
