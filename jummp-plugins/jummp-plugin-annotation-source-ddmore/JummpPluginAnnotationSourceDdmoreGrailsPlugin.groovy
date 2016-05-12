/**
 * Copyright (C) 2010-2015 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

import net.biomodels.jummp.annotation.DDMoReMetadataInputSource
import eu.ddmore.metadata.impl.MetadataInformationServiceImpl
import eu.ddmore.metadata.sparql.SparqlQueryExecutor
import eu.ddmore.metadata.sparql.SparqlQueryService

class JummpPluginAnnotationSourceDdmoreGrailsPlugin {
    final def DEFAULT_RDF_STORE_URL ="http://open-physiology.org:3030/demo2/query"
    // the plugin version
    def version = "0.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.3 > *"
    def loadAfter = [
            'jummp-plugin-core-api',
            'jummp-plugin-security',
            'jummp-plugin-configuration',
            'jummp-plugin-annotation-core',
            'jummp-plugin-web-application'
    ]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def title = "Jummp Plugin Annotation Source DDMoRe Plugin" // Headline display name of the plugin
    def description = '''\
This plugin uses the DDMoRe Metadata Integration Service to discover the metadata
statements that can be expressed about a model in PharmML.
'''

    // URL to the plugin's documentation
    def documentation = "http://bitbucket.org/jummp/jummp"

    def license = "AGPL3"

    def organization = [
        name: "EMBL-European Bioinformatics Institute",
        url: "http://www.ebi.ac.uk/"
    ]

    def developers = [
        [ name: "Mihai Glonț", email: "mihai.glont@ebi.ac.uk" ]
    ]

    def issueManagement = [ system: "JIRA", url: "https://jummp-repo.atlassian.net/" ]

    def scm = [ url: "http://bitbucket.org/jummp/jummp/" ]
    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before
    }

    def doWithSpring = {
        def cfg = application.config.jummp.ddmore.rdfstore.url
        String url
        if (!cfg) {
            println "WARN\tDid you forget to define the setting jummp.ddmore.rdfstore.url?"
            url = DEFAULT_RDF_STORE_URL
        } else {
            url = cfg
        }
        println "INFO\tUsing DDMoRe RDF Store at $url to retrieve annotation schema."
        metadataInputSource(DDMoReMetadataInputSource) {
            service = new MetadataInformationServiceImpl(new SparqlQueryExecutor(
                    new SparqlQueryService(url)))
        }
    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { ctx ->
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
