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

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured

@Secured(["IS_AUTHENTICATED_FULLY"])
class JummpController {

    def userService
    def grailsApplication
    def teamService

    final List<String> AUDIT_EXCEPTIONS = ['support', 'aboutus', 'contactus', 'lookupUser',
                                           'autoCompleteUser', 'teamLookup']
    String theme

    //def beforeInterceptor = [action: this.&detectTheme, except: AUDIT_EXCEPTIONS]

    private void detectTheme() {
        theme = grailsApplication.config.jummp.branding.style
        if (!theme)
            theme = 'default'
    }
    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def support() {
        detectTheme()
        [messageCode: "jummp.support.${theme}.message",
         titleCode: "jummp.support.${theme}.title"]
    }

    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def aboutus() {
        detectTheme()
        [messageCode: "jummp.aboutus.${theme}.message",
         titleCode: "jummp.aboutus.${theme}.title"]
    }

    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def contactus() {
        detectTheme()
        [messageCode: "jummp.contactus.${theme}.message",
         titleCode: "jummp.contactus.${theme}.title"]
    }

    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def termsOfUse() {
        detectTheme()
        [messageCode: "jummp.termsOfUse.${theme}.message",
         titleCode: "jummp.termsOfUse.${theme}.title"]
    }

    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def howToCiteBioModelsDatabase() {
        detectTheme()
        render(view: "howToCite", model: [titleCode: "jummp.howToCite.${theme}.title"])
    }

    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def acknowledgements() {
        detectTheme()
        render(view: "acknowledgements", model: [titleCode: "jummp.acknowledgements.${theme}.title"])
    }

    def lookupUser = {
        if (params.name) {
            String user = userService.getUsername(params.name)
            if (user) {
                render (['found': true, 'username':user] as JSON)
            }
        }
        render (['found': false] as JSON)
    }

    def autoCompleteUser = {
        def usersFound = userService.searchUsers(params.term)
        render (usersFound as JSON)
    }

    def teamLookup = {
		if (params.teamID) {
			long teamID;
			try {
				teamID = Long.parseLong(params.teamID)
			}
			catch(Exception e) {
				e.printStackTrace();
				render "Invalid team specified"
				return;
			}
			def users = teamService.getUsersFromTeam(teamID)
    		render (users as JSON)
    	}
    	render "No team specified"
    }
}
