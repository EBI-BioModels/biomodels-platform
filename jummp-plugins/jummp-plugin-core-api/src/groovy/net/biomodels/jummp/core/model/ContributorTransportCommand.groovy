/**
 * Copyright (C) 2010-2022 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
package net.biomodels.jummp.core.model

import net.biomodels.jummp.model.ContributionRole
import net.biomodels.jummp.plugins.security.Person
import net.biomodels.jummp.plugins.security.User

/**
 * @short A simple wrapper class for interacting services with contributors.
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
class ContributorTransportCommand {
    User user
    Person person
    ContributionRole role
    boolean external
    // this property is used to disable the changing of the role for this contributor
    boolean locked

    static constraints = {
        external(nullable: true)
    }
}
