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

import grails.converters.JSON
import grails.converters.XML
import net.biomodels.jummp.core.IModelService
import net.biomodels.jummp.webapp.rest.marshaller.ModelXmlMarshaller
import net.biomodels.jummp.webapp.rest.model.show.Model

class JummpPluginWebApplicationGrailsPlugin {
    // the plugin version
    def version = "0.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.0 > *"
    // the other plugins this plugin depends on
    def loadAfter = ["jummp-plugin-security", "jummp-plugin-core-api"]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def title = "Jummp Plugin Web Application Plugin" // Headline display name of the plugin
    def author = "Your name"
    def authorEmail = ""
    def description = '''\
Brief summary/description of the plugin.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/jummp-plugin-web-application"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
//    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
//    def organization = [ name: "My Company", url: "http://www.my-company.com/" ]

    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
//    def scm = [ url: "http://svn.grails-plugins.codehaus.org/browse/grails-plugins/" ]

    def doWithWebDescriptor = { xml ->
        def servlets = xml.'servlet'
        def lastServlet = servlets[servlets.size() - 1]
        lastServlet + {
            'servlet' {
                'servlet-name'('DefaultServletReaderConfig')
                'servlet-class'('com.wordnik.swagger.servlet.config.DefaultServletReaderConfig')
                'init-param' {
                    'param-name'('swagger.resource.package')
                    'param-value'('net.biomodels.jummp.webapp.rest')
                }
                'init-param' {
                    'param-name'('swagger.api.basepath')
                    'param-value'(application.config.grails.serverURL)
                }
                'init-param' {
                    'param-name'('api.version')
                    'param-value'('0.1')
                }
                'init-param' {
                    'param-name'('title')
                    'param-value'(application.metadata['app.name'].toUpperCase().replace("-", " "))
                }
                'init-param' {
                    'param-name'('description')
                    'param-value'('A repository of computational models encoded in standard formats.')
                }
                'init-param' {
                    'param-name'('contact')
                    'param-value'('mihai.glont@ebi.ac.uk')
                }
                'init-param' {
                    'param-name'('licence')
                    'param-value'('AGPL3')
                }
                'init-param' {
                    'param-name'('licence-url')
                    'param-value'('https://www.gnu.org/licenses/agpl-3.0.html')
                }
                'load-on-startup'(2)
            }
            'servlet' {
                'servlet-name'('ApiDeclarationServlet')
                'servlet-class'('com.wordnik.swagger.servlet.listing.ApiDeclarationServlet')
            }
        }

        def mappings = xml.'servlet-mapping'
        def lastMapping = mappings[mappings.size() - 1]
        lastMapping + {
            'servlet-mapping' {
                'servlet-name'('ApiDeclarationServlet')
                'url-pattern'('/api-docs/*')
            }
        }
    }

    def doWithSpring = {
    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { applicationContext ->
        // TODO Implement post initialization spring config (optional)
        XML.registerObjectMarshaller(new ModelXmlMarshaller())
        JSON.registerObjectMarshaller(Model) { Model M ->
            Map result = [:]
            result['submissionIdentifier'] = M.submissionId
            if (M.publicationId) {
                result['publicationIdentifier'] = M.publicationId
            }

            ['name', 'description', 'publication', 'format', 'files', 'history'].each { field ->
                final def VALUE = M."$field"
                if (VALUE) {
                    result[field] = VALUE
                }
            }
            return result
        }
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    def onShutdown = { event ->
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
