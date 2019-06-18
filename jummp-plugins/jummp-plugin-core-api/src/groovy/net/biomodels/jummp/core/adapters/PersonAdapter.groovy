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

package net.biomodels.jummp.core.adapters

import net.biomodels.jummp.core.user.PersonTransportCommand
import net.biomodels.jummp.plugins.security.Person

/**
 * @short Adapter class for the Person domain class
 *
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 */
class PersonAdapter {
    Person person

    PersonTransportCommand toCommandObject() {
        new PersonTransportCommand(id: person.id, userRealName: person.userRealName,
            institution: person.institution, orcid: person.orcid)
    }
}
