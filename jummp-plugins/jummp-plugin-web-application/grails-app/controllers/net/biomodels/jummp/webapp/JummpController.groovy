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
    def springSecurityService
    def userService
    def grailsApplication
    def teamService
    def feedbackService
    def messageSource

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
    def faq() {
        detectTheme()
        String titlePage = messageSource.getMessage("jummp.faq.${theme}.title", null, Locale.ENGLISH)
        titlePage += " | BioModels"
        render(view: "faq",
            model: [titleCode: "jummp.faq.${theme}.title", titlePage: titlePage])
    }

    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def courses() {
        detectTheme()
        render(view: "courses", model: [titleCode: "jummp.courses.${theme}.title"])
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

    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def jobs() {
        detectTheme()
        render(view: "jobs", model: [titleCode: "jummp.jobs.${theme}.title"])
    }

    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def developerZone() {
        detectTheme()
        render(view: "developerZone", model: [titleCode: "jummp.developerZone.${theme}.title"])
    }

    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def feedback() {
        if (params.star) {
            byte star = params.byte("star")
            String email = params.email
            String comment = params.comment
            if (star < 1 && star > 5) {
                render([status: '500', message: "Please rate between 1 and 5 stars."] as JSON)
            } else {
                // save the data to the database
                boolean result = feedbackService.persist(star, email, comment)
                if (result) {
                    def notification = [
                        star: star,
                        email: email,
                        comment: comment,
                        user: springSecurityService.currentUser
                    ]
                    sendMessage("seda:jummp.feedback", notification)
                    render([status: '200', message: "Thank you for your feedback."] as JSON)
                } else {
                    render([status: '500', message: "It looks like you provided that feedback before. Please try again with a different message."] as JSON)
                }
            }
        } else {
            println "This operation does not support."
        }
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
				teamID = params.long("teamID")
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
