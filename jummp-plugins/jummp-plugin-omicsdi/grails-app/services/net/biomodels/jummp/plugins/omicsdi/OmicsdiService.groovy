/**
 * Copyright (C) 2010-2016 EMBL-European Bioinformatics Institute (EMBL-EBI),
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





package net.biomodels.jummp.plugins.omicsdi

import grails.util.Environment
import groovy.json.JsonBuilder
import org.perf4j.aop.Profiled

/**
 * @short Omicsdi class for managing OmicsDI's settings
 *
 * This class provides means of handling functionality for searching models based on OmicsDI's API.
 * It accesses the database and get essential data that are included into XML files.
 * Otherwise, we implement the methods configuring OmicsDI API.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */

class OmicsdiService {
    private final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()
    private final boolean IS_ERROR_ENABLED = log.isErrorEnabled()
    private final boolean IS_INFO_ENABLED = log.isInfoEnabled()
    /**
     * Dependency Injection of Metadata Delegate Service
     **/
    def metadataDelegateService
    /**
     * Dependency Injection of Spring Security Service
     **/
    def springSecurityService
    /**
     * Dependency Injection of Grails Application
     **/
    def grailsApplication
    /*
     * Dependency injection of the configuration service
     */
    def configurationService

    // TODO: Define an OmicsDI XML based schema from OmicsDI specification, instead of holding a class
    static final String GRAILS_CONF_LOCATION = "grails-app/conf"
    static final String OMICSDI_CONFIG_LOCATION = "omicsdi"

    @Profiled(tag="omicsdiService.loadSchemaXml")
    String loadSchemaXml() {
        return GRAILS_CONF_LOCATION.concat("/").concat(OMICSDI_CONFIG_LOCATION)
    }

    File saveOmicsdiExportSettings(def options) {
        def dsConfig = grailsApplication.config.dataSource
        String searchStrategy = grailsApplication.config.jummp.search.strategy
        String exportFolder = grailsApplication.config.jummp.search.exportFolder
        String dbUrl = dsConfig?.url
        String dbUsername = dsConfig?.username
        String dbPassword = dsConfig?.password
        def dbSettings = ['url': dbUrl, 'username': dbUsername, 'password': dbPassword]
        def builder = new JsonBuilder()
        String serverUrl = "http://www.ebi.ac.uk/biomodels-main"
        if (Environment.current != Environment.PRODUCTION) {
            serverUrl = grailsApplication.config.grails.serverURL
        }
        def partialData = [
            'repositoryName' : grailsApplication.config.jummp.metadata.officialDatabaseName,
            'serverUrl' : serverUrl
        ]
        builder(partialData: partialData,
            'folder': exportFolder,
            'jummpPropFile': configurationService.getConfigFilePath(),
            'searchStrategy': searchStrategy,
            'database': dbSettings,
            'options': options)
        File indexingData = new File(exportFolder, "omicsdiSettings.json")
        indexingData.setText(builder.toPrettyString())
        return indexingData
    }

    void exportOmicsdiEntries(def options) {
        File indexingData = saveOmicsdiExportSettings(options)
        String jarJummpIndexerPath = grailsApplication.config.jummp.search.pathToIndexerExecutable

        def argsMap = [jarPath: jarJummpIndexerPath, jsonPath: indexingData.getCanonicalPath(), omicsdi: "OmicsDIXml"]

        String httpProxy = System.getProperty("http.proxyHost")
        if (httpProxy) {
            String proxyPort = System.getProperty("http.proxyPort") ?: '80'
            String nonProxyHosts = "'${System.getProperty("http.nonProxyHosts")}'"
            StringBuilder proxySettings = new StringBuilder()
            proxySettings.append(" -Dhttp.proxyHost=").append(httpProxy).append(
                " -Dhttp.proxyPort=").append(proxyPort).append(" -Dhttp.nonProxyHosts=").append(
                nonProxyHosts)
            argsMap['proxySettings'] = proxySettings.toString()
            if (IS_INFO_ENABLED) {
                log.info("Proxy settings for the indexer are $proxySettings")
            }
        } else {
            argsMap['proxySettings'] = ""
        }
        try {
            sendMessage("seda:omicsDiExport", argsMap)
        } catch (Exception e) {
            if (IS_ERROR_ENABLED) {
                log.error("Failed to build OmicsDI XML based schema files - ${e.message}", e)
            }
        }
    }
}
