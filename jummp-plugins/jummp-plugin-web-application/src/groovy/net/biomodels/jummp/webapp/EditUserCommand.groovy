/**
 * Copyright (C) 2010-2024 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

import grails.validation.Validateable
import groovy.json.JsonSlurper
import net.biomodels.jummp.plugins.security.Person
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.webapp.NotificationType as NT
import net.biomodels.jummp.webapp.NotificationTypePreferences as NTPs
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * @short Command Object to validate the user before editing.
 * @author: Raza Ali: raza.ali@gmail.com
 */
@Validateable
class EditUserCommand implements Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(EditUserCommand.class)
    private static final long serialVersionUID = 1L
    String username
    String userRealName
    String email
    String institution
    String orcid

    /**
     * For example:
     * 1 Publish -> Notify: 1, Email: 0
     * 2 Revision Created -> Notify 1, Email: 0
     * 3 Access Granted -> Notify 1, Email: 1
     * options are patterned as { 1:[1,0]; 2:[1,0]; 3:[1,1]; ... }
     */
    String options

    static constraints = {
        username(nullable: false, blank: false)
        userRealName(nullable: false, blank: false)
        email(nullable: false, blank: false, email: true)
        institution(nullable: true)
        orcid nullable: true, validator: {
            if (it) {
                Pattern p = Pattern.compile("^\\d{4}-\\d{4}-\\d{4}-\\d{3}(\\d|X)\$");
                Matcher m = p.matcher(it);
                return m.matches()
            }
            return true
        }
    }

    /**
     * @return The command object as a User
     */
    User toUser() {
        Person person = new Person(userRealName: this.userRealName, institution: this.institution, orcid: this.orcid)
        User user = new User(username: this.username, person: person, email: this.email)
        user
    }

    List<NTPs> getPreferences(User user, final String options = null) {
        def lstOptions = new JsonSlurper().parseText(options)
        Map<Integer, Object> mapOptions = new HashMap<>()
        for (option in lstOptions) {
            //println option
            JSONObject jsonObject = new JSONObject(option)
            mapOptions.put(jsonObject['id'] as int, jsonObject)
            LOGGER.info("${jsonObject['id']}|${jsonObject['slug']}\t\t\t${jsonObject['notify']}|${jsonObject['email']}")
        }
        List<NTPs> preferences = new LinkedList<NTPs>()
        final int nbNotificationTypes = NT.values().length
        boolean sendEmail, sendNotification
        for (int i = 1; i <= nbNotificationTypes; i++) {
            NT type = NT.getById(i)
            sendNotification = mapOptions.get(i)['notify'] == 1
            sendEmail = mapOptions.get(i)['email'] == 1
            NTPs pref = new NTPs(user: user, notificationType: type, sendMail: sendEmail, sendNotification: sendNotification)
            preferences.add(pref)
        }
        preferences
    }

    EditUserCommand sanitise() {
        EditUserCommand cmd = new EditUserCommand()
        cmd.username = this.username.decodeHTML()
        cmd.userRealName = this.userRealName.decodeHTML()
        cmd.institution = this.institution.decodeHTML()
        cmd.email = this.email.decodeHTML()
        cmd.orcid = this.orcid.decodeHTML()
        cmd
    }
}
