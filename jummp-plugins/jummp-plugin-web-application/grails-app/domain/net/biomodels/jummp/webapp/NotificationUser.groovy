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
 * @short Representation of the Notification User relationship.
 * This class is the representation of a Notification send to a User.
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 */
class NotificationUser implements Serializable {
    private static final long serialVersionUID = 1L

    Notification notification
    User user
    boolean notificationSeen = false

    static constraints = {
        notification(nullable: false, unique: false)
        user(nullable: false, unique: false)
        notificationSeen(nullable: false, unique: false)
    }
}
