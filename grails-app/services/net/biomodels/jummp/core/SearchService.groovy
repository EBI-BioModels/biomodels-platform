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

package net.biomodels.jummp.core

import grails.async.Promise
import grails.plugins.springsecurity.Secured
import groovy.json.JsonBuilder
import java.util.concurrent.atomic.AtomicReference
import net.biomodels.jummp.core.adapters.DomainAdapter
import net.biomodels.jummp.core.events.LoggingEventType
import net.biomodels.jummp.core.events.PostLogging
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.ModelFormat
import net.biomodels.jummp.model.Revision
import org.apache.commons.lang.StringUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrDocumentList
import org.apache.solr.common.SolrInputDocument
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.perf4j.aop.Profiled
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.acls.domain.BasePermission
import org.apache.solr.client.solrj.SolrServer

/**
 * @short Singleton-scoped facade for interacting with a Solr instance.
 *
 * This service provides means of indexing and querying generic information about
 * models.
 *
 * @author Raza Ali, raza.ali@ebi.ac.uk
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @date   20150611
 */
class SearchService {
    static final String[] SOLR_SPECIAL_CHARACTERS = ["+", "-", "&", "|", "!", "(", ")",
            "{", "}", "[", "]", "^", "\"", "~", "*", "?", ":", "\\"] as String[]
    static final String[] SOLR_REPLACEMENT_CHARACTERS = ["\\+", "\\-", "\\&", "\\|",
            "\\!", "\\(", "\\)", "\\{", "\\}", "\\[", "\\]", "\\^", "\\\"", "\\~", "\\*",
            "\\?", "\\:", "\\\\"] as String[]
    /**
     * The class logger.
     */
    static final Log log = LogFactory.getLog(SearchService)
    /**
     * Flag indicating the logger's verbosity threshold.
     */
    static final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()
    /**
     * Flag indicating the logger's verbosity threshold.
     */
    static final boolean IS_INFO_ENABLED = log.isInfoEnabled()
    /**
     * Disable default transactional behaviour.
     */
    static transactional = false
    /**
     * Dependency injection of ModelService.
     */
    def modelService
    /**
     * Dependency injection of SpringSecurityService.
     */
    def springSecurityService
    /**
     * Dependency injection of SolrServerHolder
     */
    def  solrServerHolder
    /*
     * Dependency injection of grailsApplication
     */
    def grailsApplication
    /*
     * Dependency injection of the configuration service
     */
    def configurationService
    /**
     * Dependency injection of miriamService.
     */
    def miriamService
    /**
     * Dependency injection of aclUtilService
     */
    def aclUtilService

    /**
     * Clears the index. Handle with care.
     */
    @Secured(['ROLE_ADMIN'])
    @PostLogging(LoggingEventType.DELETION)
    @Profiled(tag="searchService.clearIndex")
    void clearIndex() {
        if (IS_DEBUG_ENABLED) {
            log.debug "Clearing the search index."
        }
        solrServerHolder.server.deleteByQuery("*:*")
        log.info "Cleared the search index."
    }

    List<String> fetchFilesFromRevision(RevisionTransportCommand rev, boolean filterMains) {
        if (filterMains) {
            return rev?.files?.findAll{it.mainFile}.collect{it.path}
        }
        return rev?.files?.collect{it.path}
    }

    /**
     * Adds a revision to the index
     *
     * Adds the specified @param revision to the index.
     * @param revision The revision to be indexed
     **/
    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="searchService.updateIndex")
    void updateIndex(RevisionTransportCommand revision) {
        String name = revision.name ?: ""
        String description = revision.description ?: ""
        String submissionId = revision.model.submissionId
        String publicationId = revision.model.publicationId ?: ""
        int versionNumber = revision.revisionNumber
        final String uniqueId = "${submissionId}.${versionNumber}"
        String exchangeFolder = new File(revision.files.first().path).getParent()
        String registryExport = miriamService.registryExport.canonicalPath
        def dsConfig = grailsApplication.config.dataSource
        String dbUrl = dsConfig?.url
        String dbUsername = dsConfig?.username
        String dbPassword = dsConfig?.password
        def dbSettings = [ 'url': dbUrl, 'username': dbUsername, 'password': dbPassword ]
        def builder = new JsonBuilder()
        def partialData=[
                'submissionId':submissionId,
                'publicationId':publicationId,
                'name':name,
                'description':description,
                'modelFormat':revision.format.name,
                'levelVersion':revision.format.formatVersion,
                'submitter':revision.owner,
                'submitterUsername': revision.model.submitterUsername,
                'publicationTitle':revision.model.publication ?
                        revision.model.publication.title : "",
                'publicationAbstract':revision.model.publication ?
                        revision.model.publication.synopsis : "",
                'publicationAuthor': revision.model.publication?.authors ?
                        revision.model.publication.authors.collect {
                            it.userRealName }.join(', ') : "",
                'publicationYear': revision.model.publication?.year ?: 0,
                'model_id':revision.model.id,
                'revision_id': revision.id,
                'deleted': revision.model.deleted,
                'public': revision.model.firstPublished ? 'true' : 'false',
                'versionNumber':versionNumber,
                'submissionDate':revision.model.submissionDate,
                'lastModified': revision.model.lastModifiedDate,
                'uniqueId':uniqueId
        ]
        builder(partialData: partialData,
            'folder':exchangeFolder,
            'mainFiles': fetchFilesFromRevision(revision, true),
            'allFiles': fetchFilesFromRevision(revision, false),
            'solrServer': solrServerHolder.SOLR_CORE_URL,
            'jummpPropFile': configurationService.getConfigFilePath(),
            'miriamExportFile': registryExport,
            'database': dbSettings)
        File indexingData = new File(exchangeFolder, "indexData.json")
        indexingData.setText(builder.toString())
        String jarPath = grailsApplication.config.jummp.search.pathToIndexerExecutable
        def argsMap = [jarPath: jarPath,
                jsonPath: indexingData.getCanonicalPath()]

        String httpProxy = System.getProperty("http.proxyHost")
        if (httpProxy) {
            String proxyPort = System.getProperty("http.proxyPort") ?: '80'
            String nonProxyHosts = "'${System.getProperty("http.nonProxyHosts")}'"
            StringBuilder proxySettings = new StringBuilder()
            proxySettings.append(" -Dhttp.proxyHost=").append(httpProxy).append(
                " -Dhttp.proxyPort=").append(proxyPort).append(" -Dhttp.nonProxyHosts=").append(
                    nonProxyHosts)
            argsMap['proxySettings'] = proxySettings.toString()
        } else {
            argsMap['proxySettings'] = ""
        }
        sendMessage("seda:exec", argsMap)
    }

    /**
     * Clears the existing index and then regenerates it.
     *
     * This method requires ROLE_ADMIN permissions.
     **/
    @Secured(['ROLE_ADMIN'])
    @PostLogging(LoggingEventType.CREATION)
    @Profiled(tag="searchService.regenerateIndices")
    void regenerateIndices() {
        clearIndex()
        List<RevisionTransportCommand> revisions = Revision.list(fetch: [model: "eager"]).collect { r ->
            DomainAdapter.getAdapter(r).toCommandObject()
        }
        if (IS_DEBUG_ENABLED) {
            log.debug "Indexing ${revisions.size()} revisions."
        }
        Authentication auth = springSecurityService.authentication
        AtomicReference<Authentication> authRef = new AtomicReference<>(auth)
        Promise p = Revision.async.task {
            SecurityContextHolder.context.authentication = authRef.get()
            revisions.each {
                try {
                    updateIndex(it)
                }
                catch(Exception e) {
                    log.error("Exception thrown while indexing ${it} ${e.getMessage()}", e)
                }
            }
        }
        p.onComplete {
            if (IS_INFO_ENABLED) {
                log.info "Finished regenerating the index."
            }
        }
        p.onError { Throwable e ->
            log.error("Error regenerating the index: ${e.message}", e)
        }
    }

    /**
     * Makes a model public at the specified revision in the solr index
     *
     * Makes the @revision public in the solr index. @revision can be domain or transport object
     **/
    void makePublic(def revision) {
        SolrInputDocument doc = getSolrDocumentFromRevision(revision)
        updateIndexBase(doc, setPublicField)
    }

    /**
     * Updates the deleted field for a given model in the Solr index.
     *
     * @param model can be domain or transport object
     * @param deleted the new value that should be put in the Solr index. Defaults
     *      to true if unspecified
     **/
    void setDeleted(def model, boolean deleted = true) {
        def searchService = grailsApplication.mainContext.searchService
        SolrDocumentList docs = findSolrDocumentByModel(model, ["deleted"])
        docs.each {
            SolrInputDocument doc = getSolrDocumentWithId(it.get("uniqueId"))
            updateIndexBase(doc, setDeletedField.rcurry(deleted))
        }
        // force commit as downstream we will query the index directly.
        solrServerHolder.server.commit()
    }

    /**
     * Checks whether a model is marked as deleted in the Solr index.
     *
     * @param model An instance of Model or ModelTransportCommand for which to check.
     * @return true if the corresponding SolrInputDocument is marked as deleted, false otherwise.
     */
    boolean isDeleted(def model) {
        SolrDocumentList docs = findSolrDocumentByModel(model, ["deleted"])
        if (0 == docs.size()) {
            return false
        } else if (1 == docs.size()) {
            return docs.first().get('deleted')
        } else {
            return null == docs.find { it.get('deleted') }
        }
    }


    /*
     * Finds the SolrDocument associated with a given model.
     *
     * The lookup is done based on the model's submission identifier, which should yield
     * a single result, but if that is not the case, this method does not truncate the
     * response from Solr, hence the reason for returning a SolrDocumentList.
     *
     * @param model either a ModelTransportCommand or a Model for which to find the SolrDocument.
     * @param fields a list of fields that should be included for each result. The 'uniqueId'
     * field is automatically added to @p fields if not already present.
     */
    private SolrDocumentList findSolrDocumentByModel(def model,
            List<String> fields = ['uniqueId']) {
        SolrQuery query = new SolrQuery()
        query.setQuery("submissionId:${model?.submissionId}")
        if (!fields.contains("uniqueId")) {
            fields.add('uniqueId')
        }
        query.setFields(fields.toArray(new String[0]))
        query.set("defType", "edismax")
        QueryResponse response = solrServerHolder.server.query(query)
        SolrDocumentList docs = response.getResults()
        if (0 == docs.size()) {
            log.warn("Could not find a Solr document for model ${model?.submissionId}")
        } else if (docs.size() > 1) {
            log.error("Multiple Solr documents corresponding to model ${model?.submissionId}")
        }
        docs
    }

    /**
     * Returns search results for query restricted Models the user has access to.
     *
     * Executes the @p query, restricting results to Models the current user has access to.
     * @param query free text search on models
     * @return Collection of ModelTransportCommand of relevant models available to the user.
     **/
    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="searchService.searchModels")
    Collection<ModelTransportCommand> searchModels(String query) {
        long start = System.currentTimeMillis()
        SolrDocumentList results = search(query)
        if (IS_DEBUG_ENABLED) {
            log.debug("Solr returned in ${System.currentTimeMillis() - start}")
        }
        start = System.currentTimeMillis()
        final int COUNT = results.size()
        Map<String, ModelTransportCommand> returnVals = new LinkedHashMap<>(COUNT + 1, 1.0f)
        boolean isAdmin = SpringSecurityUtils.ifAnyGranted("ROLE_ADMIN")
        Map<Long, Long> modelsAdded = new HashMap<Long, Long>()
        results.each {
            if (!it.containsKey("deleted") || !it.get("deleted")) {
                boolean okayToProceed = true
                boolean checkPermissions = !isAdmin
                long model_id = it.get("model_id")
                long revision_id = it.get("revision_id")
                if (!modelsAdded.containsKey(model_id) || modelsAdded.get(model_id) < revision_id) {
                    if (it.containsKey("public") && it.get("public")) {
                        checkPermissions = false
                    }
                    if (checkPermissions) {
                        Revision rev = Revision.get(revision_id)
                        okayToProceed = aclUtilService.hasPermission(
                            springSecurityService.authentication, rev, BasePermission.READ)
                    }
                    if (okayToProceed) {
                        ModelTransportCommand mtc = new ModelTransportCommand(
                            submitter: it.get("submitter"),
                            submitterUsername: it.get("submitterUsername"),
                            name: it.get("name"),
                            submissionId: it.get("submissionId"),
                            publicationId: it.get("publicationId"),
                            submissionDate: it.get("submissionDate"),
                            lastModifiedDate: it.get("lastModified"),
                            id: it.get("model_id"),
                            format: DomainAdapter.getAdapter(
                                        ModelFormat.findByName(it.get("modelFormat")))
                                        .toCommandObject()
                            )
                        returnVals.put(it.get("submissionId"), mtc)
                        modelsAdded.put(model_id, revision_id)
                    }
                }
            }
        }
        if (IS_DEBUG_ENABLED) {
            log.debug("Results processed in ${System.currentTimeMillis() - start}")
        }
        return returnVals.values()
    }

    /**
     * Internal method to execute a query.
     *
     * Queries Solr and returns the results.
     *
     * @param q The query to be executed
     * @return A list of documents corresponding to revisions that match @param q.
     **/
    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="searchService.search")
    private SolrDocumentList search(String q) {
        SolrQuery query = new SolrQuery()
        String newQuery = StringUtils.replaceEach(q, SOLR_SPECIAL_CHARACTERS,
                SOLR_REPLACEMENT_CHARACTERS)
        query.setQuery(q)
        query.setParam("defType", "edismax")
        query.setParam("tie", "0.8")
        query.setParam("q.alt", "*:*")
        QueryResponse response = solrServerHolder.server.query(query)
        SolrDocumentList docs = response.getResults()
        return docs
    }

    /**
     * Helper functions to update solr index
     */

    private void updateIndexWithDocument(SolrInputDocument doc) {
        solrServerHolder.server.add(doc)
    }

    private SolrInputDocument getSolrDocumentFromRevision(def revision) {
        String submissionId = revision.model.submissionId
        int versionNumber = revision.revisionNumber
        String id = "${submissionId}.${versionNumber}"
        return getSolrDocumentWithId(id)
    }

    private SolrInputDocument getSolrDocumentWithId(String id) {
        SolrInputDocument doc = new SolrInputDocument()
        doc.addField("uniqueId", id)
        return doc
    }

    def updateIndexBase(doc, updateToApply) {
        updateToApply(doc)
        updateIndexWithDocument(doc)
    }

    def setPublicField = { doc ->
        Map<String, String> partialUpdate = new HashMap<String, String>();
        partialUpdate.put("set", "true");
        doc.addField("public", partialUpdate);
    }

    def setDeletedField = { doc, flag = true ->
        Map<String, String> partialUpdate = new HashMap<>()
        partialUpdate.put("set", flag ? "true" : "false")
        doc.addField("deleted", partialUpdate)
    }

    /*
     *  ///End of helper functions
     **/
}

