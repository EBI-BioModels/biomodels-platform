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





package net.biomodels.jummp.search

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.transaction.NotTransactional
import grails.util.Holders
import groovy.json.JsonBuilder
import net.biomodels.jummp.core.ModelSearchStrategy
import net.biomodels.jummp.core.adapters.ModelFormatAdapter
import net.biomodels.jummp.core.events.*
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.core.model.identifier.ModelIdentifierUtils
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.Revision
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrDocumentList
import org.apache.solr.common.SolrInputDocument
import org.perf4j.aop.Profiled
import org.springframework.context.ApplicationListener
import org.springframework.security.acls.domain.BasePermission

/**
 * @short Singleton-scoped facade for interacting with a SolrServerHolder's instance.
 *
 * This class provides means of indexing and querying generic information about
 * models.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 * @date   12/09/2016
 */

class SolrBasedSearch implements ModelSearchStrategy, ApplicationListener<ModelOperationEvent> {
    /**
     * The class logger.
     */
    static final Log log = LogFactory.getLog(SolrBasedSearch)
    /**
     * Flag indicating the logger's verbosity threshold.
     */
    static final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()
    /**
     * Flag indicating the logger's verbosity threshold.
     */
    static final boolean IS_INFO_ENABLED = log.isInfoEnabled()
    /**
     * The Solr Request Handler to use for handling searches.
     */
    final String SEARCH_HANDLER = "/select"
    /**
     * Dependency injection of ModelService.
     */
    def modelService = Holders.grailsApplication.mainContext.getBean('modelService')
    /**
     * Dependency injection of SpringSecurityService.
     */
    def springSecurityService = Holders.grailsApplication.mainContext.getBean('springSecurityService')
    /**
     * Dependency injection of SolrServerHolder
     */
    def solrServerHolder = Holders.grailsApplication.mainContext.getBean('solrServerHolder')
    /*
     * Dependency injection of grailsApplication
     */
    def grailsApplication = Holders.grailsApplication.mainContext.getBean('grailsApplication')
    /*
     * Dependency injection of the configuration service
     */
    def configurationService = Holders.grailsApplication.mainContext.getBean('configurationService')
    /**
     * Dependency injection of miriamService.
     */
    def miriamService = Holders.grailsApplication.mainContext.getBean('miriamService')
    /**
     * Dependency injection of aclUtilService
     */
    def aclUtilService = Holders.grailsApplication.mainContext.getBean('aclUtilService')

    def producerTemplate = Holders.grailsApplication.mainContext.getBean('producerTemplate')

    void onApplicationEvent(ModelOperationEvent event) {
        // TODO: optimise this method to benefit the robustness of polymorphism
        // move each of if statement to associated event class
        // thinking about how to use the methods: setDeleted, isDeleted, makePublic, etc.
        if (event instanceof ModelDeletedEvent) {
            def model = event.model
            if (IS_INFO_ENABLED) {
                log.info("The model associated with the submission id $model.submissionId has been deleted.")
            }
            setDeleted(model)
            if (!isDeleted(model)) {
                // leave the model as not deleted and log the error
                log.error("Could not set model ${model.submissionId} as deleted in Solr.")
            } else {
                // quick test to make sure Solr is in sync with the database
                boolean db = model.deleted
                boolean solr = isDeleted(model)
                if (IS_DEBUG_ENABLED) {
                    def m = new StringBuilder("Deletion status for ").append(model.submissionId
                    ).append(" - db: ").append(db).append(" solr: ").append(solr)
                    log.debug(m.toString())
                }
            }
        } else if (event instanceof ModelRestoredEvent) {
            def model = event.model
            setDeleted(model, false)
            if (isDeleted(model)) {
                log.error("Could not restore model associated with the submission id ${model.submissionId} in Solr.")
            } else {
                log.info("The model associated with the submission id $model.submissionId has been restored in Solr.")
            }
        } else if (event instanceof ModelCertifiedEvent) {
            if (event.status) {
                log.info("The model associated with the submission id $event.revision.model.submissionId has been certified.")
            } else {
                log.info("The model associated with the submission id $event.revision.model.submissionId has been uncertified due to errors.")
            }
            setCertified(event.revision, event.status)
        } else if (event instanceof ModelPublishedEvent) {

        } else if (event instanceof RevisionCreatedEvent) {
            log.info("A revision of the model associated with the submission id $event.revision.model.submissionId has been created.")
        }
    }

    /**
     * Clears the index. Handle with care.
     */
    @Profiled(tag="searchService.clearIndex")
    void clearIndex() {
        if (IS_DEBUG_ENABLED) {
            log.debug "Clearing the search index."
        }
        solrServerHolder.solrClient.deleteByQuery("*:*")
        log.info "Cleared the search index."
    }

    private List<String> fetchFilesFromRevision(RevisionTransportCommand rev, boolean filterMains) {
        if (filterMains) {
            return rev?.files?.findAll{it.mainFile}.collect{it.path}
        }
        return rev?.files?.collect{it.path}
    }

    @Profiled(tag="searchService.clearIndex")
    void clearIndex(RevisionTransportCommand revisionTC) {

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
        Revision.withSession {
            String name = revision.name ?: ""
            String description = revision.description ?: ""
            String submissionId = revision.model.submissionId
            String publicationId = revision.model.publicationId ?: ""
            int versionNumber = revision.revisionNumber
            boolean isCertified = null != revision.qcInfo
            final String uniqueId = "${submissionId}.${versionNumber}"
            String exchangeFolder = new File(revision.files.first().path).getParent()
            String registryExport = miriamService.registryExport.canonicalPath
            def dsConfig = grailsApplication.config.dataSource
            def searchStrategy = grailsApplication.config.jummp.search.strategy
            String dbUrl = dsConfig?.url
            // the database connection string with unicode options is not working with Indexer
            dbUrl = ModelIdentifierUtils.simplifyDbConnStr(dbUrl)
            String dbUsername = dsConfig?.username
            String dbPassword = dsConfig?.password
            def dbSettings = [ 'url': dbUrl, 'username': dbUsername, 'password': dbPassword ]
            def builder = new JsonBuilder()
            def partialData = [
                'submissionId': submissionId,
                'publicationId' :publicationId,
                'name': name,
                'description' : description,
                'modelFormat' : revision.format.name,
                'levelVersion' : revision.format.formatVersion,
                'submitter' : revision.owner,
                'submitterUsername' :  revision.model.submitterUsername,
                'publicationTitle' : revision.model.publication ?
                    revision.model.publication.title  :  "",
                'publicationAbstract' : revision.model.publication ?
                    revision.model.publication.synopsis : "",
                'publicationAuthor': revision.model.publication?.authors ?
                    revision.model.publication.authors.collect {
                        it.userRealName }.join(', ') : "",
                'publicationYear': revision.model.publication?.year ?: 0,
                'model_id' : revision.model.id,
                'revision_id' :  revision.id,
                'deleted' :  revision.model.deleted,
                'public' :  revision.model.firstPublished ? 'true'  :  'false',
                'certified' : isCertified ? 'true' : 'false',
                'versionNumber' : versionNumber,
                'submissionDate' : revision.model.submissionDate,
                'lastModified' :  revision.model.lastModifiedDate,
                'uniqueId' : uniqueId
            ]
            builder(partialData: partialData,
                'folder': exchangeFolder,
                'mainFiles': fetchFilesFromRevision(revision, true),
                'allFiles': fetchFilesFromRevision(revision, false),
                'solrServer': solrServerHolder.SOLR_CORE_URL,
                'jummpPropFile': configurationService.getConfigFilePath(),
                'miriamExportFile': registryExport,
                'searchStrategy': searchStrategy,
                'database': dbSettings)
            File indexingData = new File(exchangeFolder, "indexData.json")
            indexingData.setText(builder.toPrettyString())

            String jarPath = grailsApplication.config.jummp.search.pathToIndexerExecutable
            def argsMap = [jarPath: jarPath, jsonPath: indexingData.absolutePath]
            argsMap.putAll(configurationService.configureProxySettings() as Map<? extends String, ? extends String>)

            try {
                producerTemplate.sendBody("seda:exec", argsMap)
            } catch (Exception e) {
                log.error("Failed to index revision $revision.properties - ${e.message}", e)
                //TODO RETRY
            }
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

    private void setCertified(def rev, boolean value = true) {
        SolrInputDocument doc = getSolrDocumentFromRevision rev
        setBooleanFieldForDocument(doc, "certified", value)
        updateIndexWithDocument(doc)
    }

    private void setBooleanFieldForDocument(SolrInputDocument d, String f, boolean v) {
        Map<String, String> cmd = [:]
        cmd.put("set", v ? 'true' : 'false')
        d.addField(f, cmd)
    }

    /**
     * Updates the deleted field for a given model in the Solr index.
     *
     * @param model can be domain or transport object
     * @param deleted the new value that should be put in the Solr index. Defaults
     *      to true if unspecified
     **/
    private void setDeleted(def model, boolean deleted = true) {
        SolrDocumentList docs = findSolrDocumentByModel(model, ["deleted"])
        docs.each {
            SolrInputDocument doc = getSolrDocumentWithId(it.get("uniqueId"))
            updateIndexBase(doc, setDeletedField.rcurry(deleted))
        }
        // force commit as downstream we will query the index directly.
        solrServerHolder.solrClient.commit()
    }

    /**
     * Checks whether a model is marked as deleted in the Solr index.
     *
     * @param model An instance of Model or ModelTransportCommand for which to check.
     * @return true if the corresponding SolrInputDocuments are marked as deleted, false otherwise.
     */
    private boolean isDeleted(def model) {
        SolrDocumentList docs = findSolrDocumentByModel(model, ["deleted"])
        if (0 == docs.size()) {
            return false
        } else if (1 == docs.size()) {
            return docs.first().get('deleted')
        } else {
            return null == docs.find { !(it.get('deleted')) }
        }
    }

    boolean isCertified(def rev) {
        SolrInputDocument doc = getSolrDocumentFromRevision(rev)
        if (!doc) {
            return false
        }
        return null != doc.get('certified')
    }

    /*
     * Finds the SolrDocuments associated with a given model.
     *
     * The lookup is done based on the model's submission identifier. This method returns
     * the SolrInputDocuments for all revisions of the given model, hence the reason for
     * returning a SolrDocumentList.
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
        QueryResponse response = solrServerHolder.solrClient.query(query)
        SolrDocumentList docs = response.getResults()
        if (0 == docs.size()) {
            log.warn("Could not find a Solr document for model ${model?.submissionId}")
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
    SearchResponse searchModels(String query, String domain, SortOrder sortOrder, Map<String, Integer> paginationCriteria) {
        //solrServerHolder.init()
        long start = System.currentTimeMillis()
        SolrDocumentList results = search(query)
        if (IS_DEBUG_ENABLED) {
            log.debug("Solr returned in ${System.currentTimeMillis() - start}")
        }
        start = System.currentTimeMillis()
        final int COUNT = results.size()
        Map<String, ModelTransportCommand> returnVals = new LinkedHashMap<>(COUNT + 1, 1.0f)
        List<ModelTransportCommand> returnModels = new ArrayList<ModelTransportCommand>()
        boolean isAdmin = SpringSecurityUtils.ifAnyGranted("ROLE_ADMIN")
        results.each {
            if (!it.containsKey("deleted") || !it.get("deleted")) {
                boolean okayToProceed = true
                boolean checkPermissions = !isAdmin
                long modelId = it.get("model_id")
                String submissionId = it.get("submission_id")
                long revisionId = it.get("revision_id")
                if (!returnVals.containsKey(submissionId)) {
                    if (it.containsKey("public") && it.get("public")) {
                        checkPermissions = false
                    }
                    if (checkPermissions) {
                        Revision rev = Revision.get(revisionId)
                        okayToProceed = aclUtilService.hasPermission(
                            springSecurityService.authentication, rev, BasePermission.READ)
                    }
                    if (okayToProceed) {
                        Model model = Model.get(modelId)
                        if (model) {
                            Revision latestRevision = modelService.getLatestRevision(model, false)
                            Revision firstRevision = model.revisions.first()
                            ModelTransportCommand mtc = new ModelTransportCommand(
                                submitter: firstRevision.owner.person.userRealName,
                                submitterUsername: firstRevision.owner.username,
                                name: latestRevision.name,
                                submissionId: model.submissionId,
                                publicationId: model.publicationId,
                                submissionDate: firstRevision.uploadDate,
                                lastModifiedDate: latestRevision.uploadDate,
                                id: model.id,
                                state: latestRevision.state,
                                format: new ModelFormatAdapter(format: latestRevision.format).toCommandObject(),
                                flagLevel: latestRevision.qcInfo?.flag
                            )
                            returnVals.put(model.submissionId, mtc)
                            returnModels.add(mtc)
                        }
                    }
                }
            }
        }
        if (IS_DEBUG_ENABLED) {
            log.debug("Results processed in ${System.currentTimeMillis() - start}")
        }

        // create the returned object
        SearchResponse searchResponse = new SearchResponse()
        searchResponse.results = returnModels
        searchResponse.facets = new HashSet<String>()
        searchResponse.totalCount = COUNT
        return searchResponse
    }

    String[] getSortFields() {
        ["relevance"]
    }

    @Override
    @NotTransactional
    Map checkIndexedData() {
        null
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
        query.setQuery(q)
        query.setRequestHandler(SEARCH_HANDLER)
        QueryResponse response = solrServerHolder.solrClient.query(query)
        SolrDocumentList docs = response.getResults()
        return docs
    }

    /**
     * Helper functions to update solr index
     */

    private void updateIndexWithDocument(SolrInputDocument doc) {
        solrServerHolder.solrClient.add(doc)
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

    private updateIndexBase(doc, updateToApply) {
        updateToApply(doc)
        updateIndexWithDocument(doc)
    }

    def setPublicField = { doc ->
        Map<String, String> partialUpdate = new HashMap<String, String>()
        partialUpdate.put("set", "true")
        doc.addField("public", partialUpdate)
    }

    def setDeletedField = { doc, flag = true ->
        Map<String, String> partialUpdate = new HashMap<>()
        partialUpdate.put("set", flag ? "true" : "false")
        doc.addField("deleted", partialUpdate)
    }

    /**
     * End of helper functions
     */
}
