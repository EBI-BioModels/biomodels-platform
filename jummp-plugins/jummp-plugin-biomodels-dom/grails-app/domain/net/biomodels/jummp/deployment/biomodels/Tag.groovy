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

import net.biomodels.jummp.plugins.security.User
import java.text.SimpleDateFormat

/**
 * Domain class for storing tags/labels used in BioModels
 *
 * The tags can be created and managed separately. This class aims to help
 * do CRUD operations against these tags.
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
class Tag implements Serializable {
    private static final long serialVersionUID = 1L

    String name
    User userCreated
    Date dateCreated
    Date dateModified

    static mapping = {
        autoTimestamp(false)
    }

    static constraints = {
        name unique: true, nullable: false
    }

    TagTransportCommand toCommandObject() {
        String dateFormat= "yyyy-MM-dd'T'HH:mm:ss"
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat)
        String dateCreated = sdf.format(dateCreated)
        String dateModified = sdf.format(dateModified)
        new TagTransportCommand(id: id, name: name, userCreated: userCreated.username,
            dateCreated: dateCreated, dateModified: dateModified)
    }
}
