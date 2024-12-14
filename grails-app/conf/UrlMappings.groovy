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

import org.springframework.security.access.AccessDeniedException

import java.util.regex.Pattern

class UrlMappings {

    def grailsApplication

    static mappings = {
        "/model/submit"(controller: "model", action: "submit")
        "/model/submission-guidelines-and-agreement"(controller: "model", action: "terms")
        "/model/create"(controller: "model", action: "create")
        "/model/update"(controller: "model", action: "update")
        "/model/uploadFile"(controller: "model", action: "uploadFile")
        "/model/publish"(controller: "model", action: "publish")
        "/share"(controller: "model", action: "share")
        "/model/identifiers"(controller: "model", action: "identifiers")
        "/model/$action/$id(.$revisionId)?" {
            controller = 'model'
            action = action
            constraints {
                id(nullable: false, matches: /[a-zA-Z\\-_0-9]+/)
                action(nullable: false)
                revisionId(matches: /\d+/)
            }
        }
        "/contributor/$action/$id(.$revisionId)?" {
            controller = "contributor"
            action = action
            constraints {
                id(nullable: false, matches: /[a-zA-Z\\-_0-9]+/)
                action(nullable: false)
                revisionId(matches: /\d+/)
            }
        }
        "/$controller/$action?/$id?" {
            constraints {
                controller(notEqual: 'model')
            }
        }
        name agedbrain: "/agedbrain" {
            controller = "feature"
            action = 'agedbrain'
        }
        name path2models: "/path2models" {
            controller = "feature"
            action = 'path2models'
        }
        name covid19: "/covid-19" {
            controller = "feature"
            action = 'covid19'
        }
        name reproducibility: "/reproducibility" {
            controller = "feature"
            action = 'reproducibility'
        }

        name sitemap: "/sitemap" {
            controller = "jummp"
            action = "sitemap"
        }
        // used for web services
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }
        "/$id(.$revisionId)?" {
            controller = "model"
            action = 'show'
            constraints {
                id(nullable: false, validator: { modelId ->
                    def registryFactory = grailsApplication.mainContext.idGeneratorRegistryFactoryBean
                    def registry = registryFactory.object
                    Pattern modelIdRegexes = registry.getRegexForAllModelIdentifiers()

                    modelIdRegexes.matcher(modelId).matches()
                })
                revisionId(matches: /\d+/)
            }
        }
        "/"(view: "/index")
        "/maintenance"(controller: 'maintenance')
        "/maintenance/turnOn"(controller: 'maintenance', action: 'turnOn')
        "400"(controller: "errors", action: "error400")
        "403"(controller: "errors", action: "error403")
        "404"(controller: "errors", action: "error404")
        "500"(controller: "errors", action: "error500")
        "500"(controller: "errors", action: "error403", exception: AccessDeniedException)
        "/models"(controller: "search", action: "list")
        "/search"(controller: "search", action: "search")
        "/omicsdi"(controller: "omicsdi", action: "index")
        "/archive"(controller: "search", action: "archive")
        "/support"(controller: "jummp", action: "support")
        "/faq"(controller: "jummp", action: "faq")
        "/courses"(controller: "jummp", action: "courses")
        "/dev"(controller: "jummp", action: "developerZone")
        "/curation-docs"(controller: "jummp", action: "curatorZone")
        "/about"(controller: "jummp", action: "aboutus")
        "/termsofuse"(controller: "jummp", action: "termsOfUse")
        "/citation"(controller: "jummp", action: "howToCiteBioModelsDatabase")
        "/contact"(controller: "jummp", action: "contactus")
	    "/acknowledgements"(controller: "jummp", action: "acknowledgements")
	    "/jobs"(controller: "jummp", action: "jobs")
        "/lookupUser"(controller: "jummp", action: "lookupUser")
        "/registration"(controller:"usermanagement", action:"registration")
        "/forgotpassword"(controller:"usermanagement", action:"forgot")
        "/user/editUser"(controller:"usermanagement", action:"edit")
        "/user/editPassword"(controller:"usermanagement", action:"editPassword")
        "/user/profile"(controller:"usermanagement", action:"profile")
        "/user/update"(controller:"usermanagement", action:"update")
        "/user"(controller:"usermanagement", action:"show")
        "/mommanagement"(controller: "modelOfTheMonth", action: "index")
        "/competition/model-of-the-year-2022"(redirect: "/competition/model-of-the-year-2023")
        "/competition/model-of-the-year-2023"(controller: "competition", action: "modelOfTheYear2023")
        "/competition/model-of-the-year-2024"(controller: "competition", action: "modelOfTheYear2024")
        "/competition/model-of-the-year-2025"(controller: "competition", action: "modelOfTheYear2025")

        "/content/model-of-the-month"(
            controller: "content", action: "showModelOfTheMonth"
        )
        "/content/news/$slug"(
            controller: "content", action: "showNewsItem"
        )

        "/api/guest/model/about"(controller:'model', action:'about')
        "/api/model/$id(.$revisionId)?" {
            controller = "model"
            action = 'show'
            constraints {
                id(nullable: false, validator: { modelId ->
                    def registryFactory = grailsApplication.mainContext.idGeneratorRegistryFactoryBean
                    def registry = registryFactory.object
                    Pattern modelIdRegexes = registry.getRegexForAllModelIdentifiers()

                    modelIdRegexes.matcher(modelId).matches()
                })
                revisionId(matches: /\d+/)
            }
        }
        "/api/model/create"(controller:'model', action:'create')
        "/api/model/generate-omex/$id(.$revisionId)?"(controller:'model', action:'generateOmex')
        "/api/model/revisionsState/$id?"(controller:'model', action:'revisionsState')
        "/api/model/model-level-metadata/$id?(.$format)?"(controller:'model',
                action:'retrieveModelLevelMetadata')
        "/api/model/annotations/$id?(.$format)?"(controller: 'model',
                action: 'loadAllAnnotations')
        "/api/model/cache-annotations-organism-on-redis/$id?(.$format)?"(controller: 'model',
                action: 'cacheAnnotationsAndOrganismOnRedis')
        "/api/post/create"(controller:'post', action:'createNewPost')
        "/api/user/list"(controller:'userAdministration', action:'list')
        "/api/submission/create"(controller: "submission", action: "create")
        "/api/submission/update"(controller: "submission", action: "update")
        "/api/curationnotes/manage"(controller: "curationNotes", action: "doAddOrUpdate")
        "/api/contributor/init"(controller: "contributor", action: "init")
        "/api/contributor/load/$id(.$revisionId)?"(controller: "contributor", action: "load")
        "/api/search/index"(controller: "search", action: "indexViaAPI")

        def cmsPrefix = "/cms"
        "$cmsPrefix/editor/$action?"(controller: "cmsEditor")
        "$cmsPrefix/editor/edit/$id?"(controller: "cmsEditor", action: "edit")
        "$cmsPrefix/content/$action?"(controller: "cmsContent")
        "$cmsPrefix/content/show/$id?"(controller: "cmsContent", action: "show")
    }
}
