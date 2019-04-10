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
 * @short Representation of one NotificationTypePreferences.
 *
 * This class is the representation of how users want to receive different types
 * of preferences.
 *
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 */
class NotificationTypePreferences implements Serializable {
    private static final long serialVersionUID = 1L
    
    NotificationType notificationType
    boolean sendMail = false
    boolean sendNotification = true
    static belongsTo = [user:User]
    
    static constraints = {
        notificationType(nullable: false, unique: false)
        sendMail(nullable: false, unique: false)
        sendNotification(nullable: false, unique: false)
    }
    
    static NotificationTypePreferences getDefault(User user, NotificationType type) {
        NotificationTypePreferences defaultPref = new NotificationTypePreferences()
        defaultPref.user = user
        defaultPref.notificationType = type
        defaultPref.save(flush: true)
        return defaultPref
    }
}
