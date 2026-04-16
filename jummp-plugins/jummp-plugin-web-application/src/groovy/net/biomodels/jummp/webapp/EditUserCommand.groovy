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
     * options are patterned as {id: 1, slug: 'model-published', text: 'Publish', notify: 1, email: 0}
     */
    String options

    private static final String HTML_METACHAR_PATTERN = /.*[<>"&].*/

    static constraints = {
        username(nullable: false, blank: false, matches: /^[a-zA-Z0-9._@\-]+$/)
        userRealName(nullable: false, blank: false, validator: { val ->
            if (val =~ HTML_METACHAR_PATTERN) return 'userRealName.invalid.html'
            return true
        })
        email(nullable: false, blank: false, email: true)
        institution(nullable: true, validator: { val ->
            if (val && val =~ HTML_METACHAR_PATTERN) return 'institution.invalid.html'
            return true
        })
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

    EditUserCommand sanitise() {
        EditUserCommand cmd = new EditUserCommand()
        cmd.username = this.username?.decodeHTML()?.trim()
        cmd.userRealName = this.userRealName?.trim()
        cmd.institution = this.institution?.trim()
        cmd.email = this.email?.decodeHTML()?.trim()
        cmd.orcid = this.orcid?.trim()
        cmd.options = this.options
        cmd
    }
}
