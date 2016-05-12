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

class JummpController {

    def userService
    def grailsApplication
    def teamService

    def feedback = {
        String theme = grailsApplication.config.jummp.branding.style
        if ('ddmore' != theme && 'biomodels' != theme) {
            theme = 'default'
        }
        [messageCode: "jummp.feedback.${theme}.message"]
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
