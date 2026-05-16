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

import grails.validation.Validateable
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.plugins.security.Person
import java.util.regex.Pattern
import java.util.regex.Matcher

/**
 * @short Command object for User registration
 */
@Validateable
class RegistrationCommand {
    String username
    String email
    String userRealName
    String institution
    String orcid

    // HTML metacharacters that enable XSS/injection attacks
    private static final String HTML_METACHAR_PATTERN = /.*[<>"&].*/

    static constraints = {
        username(nullable: false, blank: false, unique: true, matches: /^[a-zA-Z0-9._@\-]+$/)
        email(nullable: false, email: true, blank: false, unique: true)
        userRealName(nullable: false, blank: false, validator: { val ->
            if (val =~ HTML_METACHAR_PATTERN) return 'userRealName.invalid.html'
            return true
        })
        institution(nullable: true, validator: { val ->
            if (val && val =~ HTML_METACHAR_PATTERN) return 'institution.invalid.html'
            return true
        })
        orcid(nullable: true, unique: true, validator: {
        	if (it) {
        		Pattern p = Pattern.compile("^\\d{4}-\\d{4}-\\d{4}-\\d{3}(\\d|X)\$")
        		Matcher m = p.matcher(it)
        		return m.matches()
        	}
        	return true
        })
    }

    User toUser() {
        return new User(username: this.username, email: this.email,
            person: new Person(userRealName: this.userRealName, institution:this.institution, orcid:this.orcid))
    }
}
