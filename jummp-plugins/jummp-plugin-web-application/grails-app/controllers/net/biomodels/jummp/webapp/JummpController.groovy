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
import groovy.xml.MarkupBuilder
import net.biomodels.jummp.CommonController

@Secured(["IS_AUTHENTICATED_FULLY"])
class JummpController extends CommonController {
    def springSecurityService
    def userService
    def teamService
    def feedbackService
    def messageSource
    def reviewerAccountService
    def modelService

    final List<String> AUDIT_EXCEPTIONS = ['support', 'aboutus', 'contactus', 'lookupUser',
                                           'autoCompleteUser', 'teamLookup']

    //def beforeInterceptor = [action: this.&detectTheme, except: AUDIT_EXCEPTIONS]

    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def support() {
        Map model = COMMON_PROPERTIES
        model.putAll(["messageCode": "jummp.support.${theme}.message",
                      "titleCode": "jummp.support.${theme}.title"])
        model
    }

    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def faq() {
        Map model = COMMON_PROPERTIES
        String titlePage = messageSource.getMessage("jummp.faq.${theme}.title", null, Locale.ENGLISH)
        titlePage += " | BioModels"
        model.putAll(["titleCode": "jummp.faq.${theme}.title", titlePage: titlePage])
        model
    }

    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def courses() {
        Map model = COMMON_PROPERTIES
        model.put("titleCode", "jummp.courses.${theme}.title")
        model
    }

    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def aboutus() {
        Map model = COMMON_PROPERTIES
        model.putAll([messageCode: "jummp.aboutus.${theme}.message",
                      titleCode: "jummp.aboutus.${theme}.title"])
        model
    }

    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def contactus() {
        Map model = COMMON_PROPERTIES
        model.putAll([messageCode: "jummp.contactus.${theme}.message",
                      titleCode: "jummp.contactus.${theme}.title"])
        model
    }

    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def termsOfUse() {
        Map model = COMMON_PROPERTIES
        model.putAll([messageCode: "jummp.termsOfUse.${theme}.message",
                      titleCode: "jummp.termsOfUse.${theme}.title"])
        model
    }

    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def howToCiteBioModelsDatabase() {
        Map model = COMMON_PROPERTIES
        model.put("titleCode", "jummp.howToCite.${theme}.title")
        render(view: "howToCite", model: model)
    }

    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def acknowledgements() {
        Map model = COMMON_PROPERTIES
        model.put("titleCode", "jummp.acknowledgements.${theme}.title")
        render(view: "acknowledgements", model: model)
    }

    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def jobs() {
        Map model = COMMON_PROPERTIES
        model.put("titleCode", "jummp.jobs.${theme}.title")
        model
    }

    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def curatorZone() {
        Map model = COMMON_PROPERTIES
        model.put("titleCode", "jummp.curatorZone.${theme}.title")
        render(view: "curatorZone", model: model)
    }

    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def developerZone() {
        Map model = COMMON_PROPERTIES
        model.put("titleCode", "jummp.developerZone.${theme}.title")
        render(view: "developerZone", model: model)
    }

    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def feedback() {
        if (params.star) {
            byte star = params.byte("star")
            String email = params.email.decodeHTML()
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

    @Secured(["IS_AUTHENTICATED_FULLY"])
    def createReviewerAccount() {
        String modelId = params.get("id").decodeHTML()
        String message = reviewerAccountService.createAccountAndInstructions(modelId, serverURL)
        Map retMap = [modelId: modelId, message: message, serverURL: serverURL]
        render(view: "createReviewerAccount", model: retMap)
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

    /**
     * A Sitemap controller that automatically generates sitemap.xml for a grails based website.
     *
     */
    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def sitemap() {
        StringWriter writer = new StringWriter()
        MarkupBuilder mkb = new MarkupBuilder(writer)
        mkb.mkp.xmlDeclaration(version: "1.0", encoding: "UTF-8")
        mkb.urlset(xmlns: "https://www.sitemaps.org/schemas/sitemap/0.9",
            'xmlns:xsi': "http://www.w3.org/2001/XMLSchema-instance",
            'xsi:schemaLocation': "https://www.sitemaps.org/schemas/sitemap/0.9 https:///www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd") {
            addAllUrlsOfModels(mkb)
            addAllUrlsOfMainMenu(mkb)
            addAllUrlsOfModelOfTheMonth(mkb)
            addOtherUrls(mkb)
        }
        render(text: writer.toString(),contentType: "text/xml", encoding: "UTF-8")
    }

    private void addAllUrlsOfModels(MarkupBuilder mkb) {
        List<String> allPublicModelIds = modelService.getAllModelIdentifiers()
        List<String> allAutogeneratedModelIds = modelService.getAutogeneratedModelIdentifiers()
        allPublicModelIds.addAll(allAutogeneratedModelIds)
        for (String modelId : allPublicModelIds) {
            mkb.url {
                loc("https://www.ebi.ac.uk/biomodels/$modelId")
                changefreq('daily')
                priority(0.8)
            }
        }
    }

    private void addAllUrlsOfMainMenu(MarkupBuilder mkb) {
        // Browse menu
        mkb.url {
            loc("https://www.ebi.ac.uk/biomodels/search?query=*%3A*")
            changefreq('daily')
            priority(0.8)
        }
        mkb.url {
            loc("https://www.ebi.ac.uk/biomodels/parameterSearch/index?query=*%3A*&start=0&size=10&sort=model%3Aascending&is_curated=true")
            changefreq('daily')
            priority(0.8)
        }
        mkb.url {
            loc("https://www.ebi.ac.uk/biomodels/covid-19")
            changefreq('yearly')
            priority(0.8)
        }
        mkb.url {
            loc("https://www.ebi.ac.uk/biomodels/path2models")
            changefreq('yearly')
            priority(0.8)
        }
        mkb.url {
            loc("https://www.ebi.ac.uk/biomodels/goChart/index")
            changefreq('yearly')
            priority(0.8)
        }
        mkb.url {
            loc("https://www.ebi.ac.uk/biomodels/agedbrain")
            changefreq('yearly')
            priority(0.8)
        }
        mkb.url {
            loc("https://www.ebi.ac.uk/biomodels/pdgsmm/index")
            changefreq('yearly')
            priority(0.8)
        }
        mkb.url {
            loc("https://www.ebi.ac.uk/biomodels/reproducibility")
            changefreq('yearly')
            priority(0.8)
        }

        // Submit menu
        mkb.url {
            loc("https://www.ebi.ac.uk/biomodels/model/submission-guidelines-and-agreement")
            changefreq('yearly')
            priority(0.8)
        }
        mkb.url {
            loc("https://www.ebi.ac.uk/biomodels/model/submit")
            changefreq('yearly')
            priority(0.8)
        }

        // Curation menu
        mkb.url {
            loc("https://www.ebi.ac.uk/biomodels/curation/fbc")
            changefreq('yearly')
            priority(0.8)
        }

        // Help menu
        mkb.url {
            loc("https://www.ebi.ac.uk/biomodels/faq")
            changefreq('yearly')
            priority(0.8)
        }
        mkb.url {
            loc("https://www.ebi.ac.uk/biomodels/user-guide/manual.html")
            changefreq('yearly')
            priority(0.8)
        }
        mkb.url {
            loc("https://www.ebi.ac.uk/biomodels/curation-docs")
            changefreq('yearly')
            priority(0.8)
        }
        mkb.url {
            loc("https://www.ebi.ac.uk/biomodels/dev")
            changefreq('yearly')
            priority(0.8)
        }
        mkb.url {
            loc("https://www.ebi.ac.uk/biomodels/courses")
            changefreq('yearly')
            priority(0.8)
        }
        mkb.url {
            loc("https://www.ebi.ac.uk/biomodels/tools/converters/")
            changefreq('yearly')
            priority(0.8)
        }

        // About us menu
        mkb.url {
            loc("https://www.ebi.ac.uk/biomodels/termsofuse")
            changefreq('yearly')
            priority(0.8)
        }
        mkb.url {
            loc("https://www.ebi.ac.uk/biomodels/citation")
            changefreq('yearly')
            priority(0.8)
        }
        mkb.url {
            loc("https://www.ebi.ac.uk/biomodels/content/news")
            changefreq('yearly')
            priority(0.8)
        }
        mkb.url {
            loc("https://www.ebi.ac.uk/biomodels/acknowledgements")
            changefreq('yearly')
            priority(0.8)
        }
        mkb.url {
            loc("https://www.ebi.ac.uk/biomodels/jobs")
            changefreq('yearly')
            priority(0.8)
        }

        // Contact us menu
        mkb.url {
            loc("https://www.ebi.ac.uk/biomodels/contact")
            changefreq('yearly')
            priority(0.8)
        }
    }

    private void addAllUrlsOfModelOfTheMonth(MarkupBuilder mkb) {
        // the Url of the index page of the Model of the month
        mkb.url {
            loc("https://www.ebi.ac.uk/biomodels/content/model-of-the-month?all=yes")
            changefreq('yearly')
            priority(0.8)
        }
    }

    private void addOtherUrls(MarkupBuilder mkb) {
        mkb.url {
            loc("https://www.ebi.ac.uk/biomodels/competition/model-of-the-year-2023")
            changefreq('yearly')
            priority(0.8)
        }
    }
}
