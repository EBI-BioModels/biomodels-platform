/**
 * Copyright (C) 2010-2020 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
 */

import org.springframework.beans.factory.NoSuchBeanDefinitionException

class JummpPluginCommonFormatGrailsPlugin {
    // the plugin version
    def version = "0.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.5 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    def title = "Jummp Plugin for Commonly Well-Known Model Formats"
    def author = "Tung Nguyen"
    def authorEmail = "tnguyen@ebi.ac.uk"
    def description = '''\
This plugin supports to handle commonly well-known formats such as C/C++, Python, R, Java, Mathematica and Matlab
'''
    def developers = [
        [ name: "Tung Nguyen", email: "tung.nguyen@ebi.ac.uk"],
        [ name: "Mihai Glonț", email: "mihai.glont@ebi.ac.uk" ]
    ]
    // URL to the plugin's documentation
//    def documentation = "http://grails.org/plugin/jummp-plugin-common-format"
    def documentation = "https://bitbucket.org/jummp/jummp"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = "AGPL3"

    // Details of company behind the plugin (if there is one)
    def organization = [ name: "EMBL-EBI", url: "https://www.ebi.ac.uk/" ]

    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
    def issueManagement = [ system: "JIRA", url: "https://jummp-repo.atlassian.net/" ]

    // Online location of the plugin's browseable source code.
    def scm = [ url: "https://bitbucket.org/jummp/jummp" ]

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before
    }

    def doWithSpring = {
    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { ctx ->
        registerFormatSupport(ctx)
    }

    def onChange = { event ->
        registerFormatSupport(event.ctx)
    }

    def onConfigChange = { event ->
        registerFormatSupport(event.ctx)
    }

    def onShutdown = { event ->
        // TODO Implement code that is executed when the application shuts down (optional)
    }

    private void registerFormatSupport(def ctx) {
        def formats = [
            [identifier: "C_CPP", name: "C/C++", version: "*", service: "ccppFormatService"],
            [identifier: "Python", name: "Python", version: "*", service: "pythonFormatService"],
            [identifier: "Python", name: "Python", version: "2.7", service: "pythonFormatService"],
            [identifier: "Python", name: "Python", version: "3.6", service: "pythonFormatService"],
            [identifier: "R", name: "R", version: "*", service: "rlangFormatService"],
            [identifier: "Java", name: "Java", version: "*", service: "javaFormatService"],
            [identifier: "Mathematica", name: "Mathematica", version: "*", service: "mathematicaFormatService"],
            [identifier: "matlab", name: "MATLAB (Octave)", version: "*", service: "matlabFormatService", controller: "matlab"]
        ]
        try {
            def service = ctx.getBean("modelFileFormatService")
            formats.each {
                String identifier = it["identifier"]
                String name = it["name"]
                String version = it["version"]
                String formatService = it["service"]
                String formatController = it["controller"] ?: "commonFormat"
                def formatCmd = service.registerModelFormat(identifier, name, version)
                service.handleModelFormat(formatCmd, formatService, formatController)
            }
        } catch (NoSuchBeanDefinitionException ignored) {
            // running as standalone plugin
        }
    }
}
