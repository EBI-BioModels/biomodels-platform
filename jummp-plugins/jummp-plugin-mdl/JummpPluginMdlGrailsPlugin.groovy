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
*
* Additional permission under GNU Affero GPL version 3 section 7
*
* If you modify Jummp, or any covered work, by linking or combining it with
* Spring Framework (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Spring Framework used as well as
* that of the covered work.}
**/

import org.springframework.beans.factory.NoSuchBeanDefinitionException

class JummpPluginMdlGrailsPlugin {
    def version = "0.1"
    def grailsVersion = "2.3 > *"
    def loadAfter = ['jummp-plugin-pharmml']
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    def title = "Jummp Plugin Mdl Plugin" // Headline display name of the plugin
    def author = "European Bioinformatics Institute (EMBL-EBI)"
    def authorEmail = ""
    def description = '''\
Provides functionality to support models encoded in MDL.
'''

    def documentation = "http://grails.org/plugin/jummp-plugin-mdl"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = "AGPL3"

    def organization = [
        name: "EMBL-European Bioinformatics Institute",
        url: "http://www.ebi.ac.uk/"
    ]

    def developers = [
        [ name: "Mihai Glonț", email: "mihai.glont@ebi.ac.uk" ]
    ]

    def issueManagement = [ url: "https://bitbucket.org/jummp/jummp/issues" ]

    def scm = [ url: "http://bitbucket.org/jummp/jummp/" ]

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before
    }

    def doWithSpring = {
        // TODO Implement runtime spring config (optional)
    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { applicationContext ->
          try {
            def service = applicationContext.getBean("modelFileFormatService")
            ["*", "5.0.8", "7.0"].each {
                def modelFormat = service.registerModelFormat("MDL",
                        "MDL", it)
                service.handleModelFormat(modelFormat, "mdlService", "mdl")
            }
        } catch(NoSuchBeanDefinitionException e) {
            println("Cannot register MDL handler because ModelFileFormatService is not available!")
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
