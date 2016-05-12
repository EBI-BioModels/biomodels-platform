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

package net.biomodels.jummp.search

import groovy.json.JsonSlurper
import grails.util.Environment
import grails.util.Metadata
import groovy.transform.CompileStatic
import java.util.concurrent.atomic.AtomicReference
import org.apache.commons.io.FileUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.solr.client.solrj.SolrServer
import org.apache.solr.client.solrj.impl.HttpSolrServer
import org.apache.solr.client.solrj.response.SolrPingResponse
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.perf4j.aop.Profiled
import org.springframework.core.io.Resource

/**
 * @short Simple holder of the reference to the Solr server.
 *
 * This singleton-scoped bean creates and maintains a reference to the Solr server.
 * It creates separate Solr cores for testing and production based on the settings
 * defined in grails-app/conf/solr/conf. The test Solr core is deleted at the end of
 * the bean's lifecycle.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @date 20150311
 */
@CompileStatic
class SolrServerHolder {
    /**
     * The class logger.
     */
    static final Log log = LogFactory.getLog(SolrServerHolder)
    /**
     * Flag indicating the logger's verbosity threshold.
     */
    static final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()
    /**
     * Flag indicating the logger's verbosity threshold.
     */
    static final boolean IS_INFO_ENABLED = log.isInfoEnabled()
    /**
     * The application name.
     */
    String APP_NAME
    /**
     * Reference to the location of the Solr home directory.
     */
    AtomicReference<File> solrHomeRef
    /**
     * Reference to the location of the Solr core folder.
     */
    AtomicReference<File> solrCoreRef
     /**
      * Reference to the URL of the Solr instance.
      */
    AtomicReference<String> solrUrlRef
    /**
     * The default name for the Solr core used by the application.
     */
    String DEFAULT_CORE_NAME
    /**
     * The name for the Solr core used by the application during testing.
     */
    static final String TEST_CORE_NAME = "test_core"
    /**
     * The location of the application's configuration folder.
     */
    static final String GRAILS_CONF_LOCATION = "grails-app/conf"
    /**
     * The path, relative to the classpath, that contains the folder defining configuration
     * settings for a Solr core that is used by the application.
     */
    static final String SOLR_CONFIG_LOCATION = "solr"
    /**
     * Dependency injection of Grails Application.
     */
    GrailsApplication grailsApplication
    /**
     * Singleton instance of SolrServer.
     */
    SolrServer server
    /**
     * The base URL for all requests to the SOLR core.
     */
    String SOLR_CORE_URL

    @Profiled(tag="solrServerHolder.init")
    void init() {
        if (IS_DEBUG_ENABLED) {
            log.debug "Initialising solrServerHolder..."
        }
        APP_NAME = ((String) Metadata.current.get('app.name')).toLowerCase()
        solrHomeRef = new AtomicReference<>()
        solrCoreRef = new AtomicReference<>()
        solrUrlRef = new AtomicReference<>()
        DEFAULT_CORE_NAME = "${APP_NAME}_core"
        final Map grailsConfig = grailsApplication.config.flatten()
        final String SOLR_URL = grailsConfig.get("jummp.search.url")
        if (!SOLR_URL) {
            throw new IllegalStateException("""\
URL of Solr server not found. Please check the setting jummp.search.url in the config file.""")
        }
        solrUrlRef.compareAndSet(null, SOLR_URL)
        String coreName = Environment.current == Environment.TEST ? TEST_CORE_NAME :
                DEFAULT_CORE_NAME
        prepareSolrCoreSetup(coreName, grailsConfig)

        SOLR_CORE_URL = getSolrCoreUrl(SOLR_URL)
        server = new HttpSolrServer(SOLR_CORE_URL)
        if (IS_INFO_ENABLED) {
            log.info "Connected to Solr instance $SOLR_CORE_URL."
        }
        SolrPingResponse response = server.ping()
        if (IS_DEBUG_ENABLED) {
            log.debug "Solr instance response time: ${response.getQTime()}ms."
        }
        log.info "Solr server is $server"
        if (IS_DEBUG_ENABLED) {
            log.debug "... finished initialising solrServerHolder."
        }
    }

    @Profiled(tag="solrServerHolder.destroy")
    void destroy() {
        if (Environment.current == Environment.TEST) {
            Map params = ["action" : "UNLOAD", "core" : TEST_CORE_NAME ]
            Map response = doSolrCoreAdminRequest(params)
            if (IS_INFO_ENABLED) {
                log.info "Request to unload test Solr core returned status ${response.inspect()}"
            }
            File testCoreFolder = solrCoreRef.get()
            forceDeleteTestSolrCore testCoreFolder
        }
        server = null
        solrCoreRef = null
        solrUrlRef = null
        solrHomeRef = null
    }

    private void forceDeleteTestSolrCore(File testCoreFolder) {
        if (testCoreFolder && testCoreFolder.exists()) {
            boolean result = FileUtils.deleteQuietly(testCoreFolder)
            if (!result) {
                log.error "Could not delete the Solr index used in the test environment."
            }
        }
    }

    @Profiled(tag="solrServerHolder.prepareSolrCoreSetup")
    private void prepareSolrCoreSetup(String name, Map cfg) {
        final File SOLR_CORE_FOLDER = getSolrCoreFolder(cfg)
        if (IS_DEBUG_ENABLED) {
            log.debug "Using Solr core folder $SOLR_CORE_FOLDER"
        }
        if (!SOLR_CORE_FOLDER.list()) {
            if (IS_DEBUG_ENABLED) {
                log.debug "Copying schema settings for Solr core $name."
            }
            final File SOLR_CONF_FOLDER = getSolrConfigFolder()
            FileUtils.copyDirectory(SOLR_CONF_FOLDER, SOLR_CORE_FOLDER, false)
            //Cannot use Solr's CoreAdminHandler because it's in solr-core.
            Map params = ["action" : "CREATE",
                    "name" : name,
                    "instanceDir" : name,
                    "config" : "solrconfig.xml",
                    "schema" : "schema.xml",
                    "dataDir" : "data",
            ]
            Map response = doSolrCoreAdminRequest(params)
            if (IS_INFO_ENABLED) {
                log.info "Response from Solr core creation request: ${response.inspect()}"
            }
        } else {
            if (IS_DEBUG_ENABLED) {
                log.debug "Reusing existing Solr core $name."
            }
        }
    }

    @Profiled(tag="solrServerHolder.doSolrCoreAdminRequest")
    private Map doSolrCoreAdminRequest(Map paramsMap) {
        final String baseUrl = solrUrlRef.get()
        final String coresPage = "/admin/cores"
        paramsMap = paramsMap ?: [:]
        paramsMap.put("wt", "json")
        String params = "?" + paramsMap.collect{k, v -> "$k=$v"}.join("&")
        String url = baseUrl + coresPage + params
        String response = new URL(url).getText("UTF-8")
        JsonSlurper slurper = new JsonSlurper()
        Map result = (Map) slurper.parseText(response)
        return result
    }

    @Profiled(tag="solrServerHolder.getSolrCoreUrl")
    private String getSolrCoreUrl(String solrUrl) {
        if (!solrUrl?.trim()) {
            throw new IllegalArgumentException("""\
Unable to work out the URL of the Solr core because the base Solr URL is undefined.""")
        }
        final String SOLR_CORE_URL
        if (Environment.current == Environment.TEST) {
            SOLR_CORE_URL = "$solrUrl/$TEST_CORE_NAME"
        } else {
            SOLR_CORE_URL = "$solrUrl/$DEFAULT_CORE_NAME"
        }
        return SOLR_CORE_URL
    }

    @Profiled(tag="solrServerHolder.ensureSolrCoreFolderExists")
    private void ensureSolrCoreFolderExists(File core) {
        if (!core) {
            throw new IllegalArgumentException("SOLR core folder cannot be undefined.")
        }
        if (core.exists() && core.canWrite()) {
            if (IS_INFO_ENABLED) {
                log.info "Reusing existing Solr core folder $core"
            }
            return
        }
        if (IS_INFO_ENABLED) {
            log.info "Creating Solr core $core"
        }
        boolean solrCoreCreated = core.mkdirs()
        if (!solrCoreCreated) {
            throw new IllegalStateException("Cannot create folder $core to store search index.")
        }
        if (!core.canWrite()) {
            throw new IllegalStateException("Missing write permission on $core.")
        }
    }

    @Profiled(tag="solrServerHolder.getSolrHomeFolder")
    private File getSolrHomeFolder(Map appConfig) {
        if (solrHomeRef.get() == null) {
            final String SOLR_HOME = appConfig.get("jummp.search.folder") ?:
                    System.getenv("SOLR_HOME")
            if (!SOLR_HOME) {
                throw new IllegalStateException("""\
The location of your Solr installation was not found. Please either set the $SOLR_HOME \
environment variable, or specify the setting jummp.search.folder in the config file.""")
            }
            final File SOLR_HOME_FOLDER = new File(SOLR_HOME)
            // need to check the canonical version to resolve symlinks
            boolean isFolder = SOLR_HOME_FOLDER.getCanonicalFile().isDirectory()
            if (!isFolder) {
                throw new IllegalStateException("Location $SOLR_HOME_FOLDER must be a folder.")
            }
            if (!SOLR_HOME_FOLDER.exists()) {
                throw new IllegalStateException("""\
Location $SOLR_HOME_FOLDER does not exist. If you do not want to create it, you can either edit \
the setting jummp.search.folder in the config file, or change the SOLR_HOME environment \
variable. If the former setting is specified,the environment variable is ignored.""")
            }
            solrHomeRef.compareAndSet(null, SOLR_HOME_FOLDER)
        }
        return solrHomeRef.get()
    }

    @Profiled(tag="solrServerHolder.getSolrCoreFolder")
    private File getSolrCoreFolder(Map cfg) {
        if (!solrCoreRef.get()) {
            if (IS_INFO_ENABLED) {
                log.info "Initialising Solr core folder."
            }
            File parent = getSolrHomeFolder(cfg)
            final File CORE_FOLDER
            if (Environment.current == Environment.TEST) {
                CORE_FOLDER = new File(parent, TEST_CORE_NAME)
                log.debug "Core folder is $CORE_FOLDER"
                forceDeleteTestSolrCore CORE_FOLDER
            } else {
                CORE_FOLDER = new File(parent, DEFAULT_CORE_NAME)
            }
            ensureSolrCoreFolderExists(CORE_FOLDER)
            solrCoreRef.compareAndSet(null, CORE_FOLDER)
        }
        return solrCoreRef.get()
    }

    @Profiled(tag="solrServerHolder.getSolrConfigFolder")
    private File getSolrConfigFolder() {
        File result
        if (Environment.isWarDeployed()) {
            final String SOLR_CONFIG_CLASSPATH_LOCATION =
                    "WEB-INF/classes/$SOLR_CONFIG_LOCATION"
            Resource resource = grailsApplication.mainContext.getResource(
                    SOLR_CONFIG_CLASSPATH_LOCATION)
            result = resource.getFile()
        } else {
            result = new File("$GRAILS_CONF_LOCATION/$SOLR_CONFIG_LOCATION")
        }
        if (!result.exists()) {
            def msg = """\
Missing schema and configuration file for Solr core in ${result.absolutePath}.""".toString()
            throw new IllegalArgumentException(msg)
        }
        if (IS_DEBUG_ENABLED) {
            log.debug "Solr core configuration templates are located in $result"
        }
        return result
    }
}
