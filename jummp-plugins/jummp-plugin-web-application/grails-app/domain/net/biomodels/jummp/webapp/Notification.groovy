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





package net.biomodels.jummp.webapp
import net.biomodels.jummp.plugins.security.User
/**
 * @short Representation of one Notification.
 * This class is the representation of one Notification.
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
class Notification implements Serializable {
    private static final long serialVersionUID = 1L

    String title
    String body
    Date dateCreated
    NotificationType notificationType
    User sender


    static constraints = {
        title(nullable: false, blank: false, unique: false)
        body(nullable: false, blank: false, unique: false, maxSize: 1024)
        notificationType(nullable: false, blank: false, unique: false)
        sender(nullable: false, blank: false, unique: false)
    }
}
