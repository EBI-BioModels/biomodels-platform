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

import grails.util.Holders
import groovy.json.JsonBuilder
import net.biomodels.jummp.annotationstore.ResourceReference
import net.biomodels.jummp.core.ModelSearchStrategy
import net.biomodels.jummp.core.adapters.ModelAdapter
import net.biomodels.jummp.core.adapters.ModelFormatAdapter
import net.biomodels.jummp.core.events.ModelOperationEvent
import net.biomodels.jummp.core.model.ModelFormatTransportCommand
import net.biomodels.jummp.core.model.ModelState
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.qcinfo.FlagLevel
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.context.ApplicationListener
import org.springframework.security.acls.domain.BasePermission
import uk.ac.ebi.ddi.ebe.ws.dao.client.dataset.DatasetWsClient
import uk.ac.ebi.ddi.ebe.ws.dao.config.AbstractEbeyeWsConfig
import uk.ac.ebi.ddi.ebe.ws.dao.config.EbeyeWsConfigDev
import uk.ac.ebi.ddi.ebe.ws.dao.model.common.Entry
import uk.ac.ebi.ddi.ebe.ws.dao.model.common.Facet
import uk.ac.ebi.ddi.ebe.ws.dao.model.common.FacetValue
import uk.ac.ebi.ddi.ebe.ws.dao.model.common.QueryResult

/**
 * @short Singleton-scoped facade for interacting with a OmicsdiHolder's instance.
 *
 * This class provides means of indexing and querying generic information about
 * models.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 * @date   12/09/2016
 */

class OmicsdiBasedSearch implements ModelSearchStrategy, ApplicationListener<ModelOperationEvent> {
    /**
     * The class logger.
     */
    static final Log log = LogFactory.getLog(OmicsdiBasedSearch.class)
    /**
     * Flag indicating the logger's verbosity threshold.
     */
    static final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()
    /**
     * Flag indicating the logger's verbosity threshold.
     */
    static final boolean IS_INFO_ENABLED = log.isInfoEnabled()
    /**
     * The OmicsDI Request Handler to use for handling searches.
     */
    def omicsDiHolder
    /**
     * Dependency injection of ModelService.
     */
    def modelService = Holders.grailsApplication.mainContext.getBean('modelService')
    /**
     * Dependency injection of SpringSecurityService.
     */
    def springSecurityService = Holders.grailsApplication.mainContext.getBean('springSecurityService')
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
        // look at solrbasedsearch
    }

    void clearIndex() {
        // Delete indexing plans from the database
        if (IS_DEBUG_ENABLED) {
            log.debug "Clearing the indexing plans."
        }
        Revision.executeUpdate("delete IndexingPlan")
    }

    SearchResponse searchModels(String query,
            Map<String, Integer> paginationCriteria = ["start": 0, "length": 50, "facetCount": 10] ) {
        long start = System.currentTimeMillis()
        AbstractEbeyeWsConfig ebeyeWsConfig = new EbeyeWsConfigDev()
        DatasetWsClient datasetWsClient = new DatasetWsClient(ebeyeWsConfig)
        // TODO: should allow searching information of other fields
        // create the returned object
        SearchResponse searchResponse = new SearchResponse()
        String[] fields = ["name", "description"]
        QueryResult result = datasetWsClient.getDatasets("biomodels", query, fields, null, null,
            paginationCriteria['start'], paginationCriteria['length'], paginationCriteria['facetCount'])
        List<Entry> entries = result.getEntries()
        List<Facet> facets = []
        int totalCount = result.count
        // convert all the returned entries to ModelTransportCommand objects
        HashSet<ModelTransportCommand> results = new HashSet<ModelTransportCommand>()
        // TODO: replace them with the actual models when biomodels importer finishes,
        // the following aims to create fake data
        List<Revision> publicRevisions = Revision.findAllByState(ModelState.PUBLISHED)
        // or get the list revisions can be retrieved by the current logged in user

        Model firstPublicModel
        Revision first
        Revision latest
        if (publicRevisions) {
            // get the first public revision among these public ones
            latest = publicRevisions.first()
            firstPublicModel = latest.getModel()
            // retrieve the first revision of the model containing it and the above latest
            first = firstPublicModel.revisions.first()
            // entries/models
            entries.eachWithIndex { Entry entry, int i ->
                String submissionId = entry.id
                Model thisModel = ModelAdapter.findByPerennialIdentifier(submissionId) ?: firstPublicModel //TODO fixme
                submissionId = thisModel.submissionId
                boolean isAccessible =
                    aclUtilService.hasPermission(springSecurityService.authentication, thisModel, BasePermission.READ)
                if (submissionId != firstPublicModel.submissionId && isAccessible) {
                    first = Revision.findByModelAndRevisionNumber(thisModel, 1)
                    latest = modelService.getLatestRevision(thisModel, false)
                }
                boolean haveName = entry.getFields().get('name')?.length > 0
                String name
                if (haveName) {
                    name = entry.getFields().get('name')[0]
                } else {
                    log.warn("The search index entry for Model ${submissionId} did not contain the model name")
                    name = latest.name
                }
                String description = latest?.description ?: ""
                User submitter = first.owner
                String submitterName = submitter.person.userRealName
                String submitterUsername = submitter.username
                String publicationId = thisModel.publicationId
                Date uploadDate = first.uploadDate
                Date modifiedDate = latest.uploadDate
                Long id = thisModel.id
                ModelState state = latest.state
                ModelFormatTransportCommand format =
                    new ModelFormatAdapter(format: latest.format).toCommandObject()
                FlagLevel qcFlag = latest.qcInfo?.flag

                ModelTransportCommand mtc = new ModelTransportCommand(
                    submitter: submitterName,
                    submitterUsername: submitterUsername,
                    name: name,
                    description: description,
                    submissionId: submissionId,
                    publicationId: publicationId,
                    submissionDate: uploadDate,
                    lastModifiedDate: modifiedDate,
                    id: id,
                    state: state,
                    format: format,
                    flagLevel: qcFlag
                )
                results.add(mtc)
            }
            // facets
            boolean ignoredFacets = false
            result.facets?.each { Facet facet ->
                ignoredFacets = facet.label.equalsIgnoreCase("repository") || facet.label.equalsIgnoreCase("source")
                if (!ignoredFacets) {
                    facets.add(facet)
                }
            }
        } else {
            totalCount = 0
            results = []
            facets = []
        }
        Set<String> immutableFacets = ["Organisms", "Publication Date", "Omics type"]
        facets.findAll({ Facet f ->
            !(immutableFacets.contains(f.label))
        })*.facetValues.flatten().each { FacetValue value ->
            String currentLabel = value.label
            if (currentLabel.contains(" ")) {
                return
            }
            ResourceReference reference = ResourceReference.findWhere(accession: currentLabel)
            String referenceName = reference?.name
            if (referenceName) {
                value.setLabel(referenceName)
            }
        }
        if (IS_DEBUG_ENABLED) {
            log.debug("Results processed in ${System.currentTimeMillis() - start}")
        }
        searchResponse.results = results
        searchResponse.facets = facets
        searchResponse.totalCount = totalCount
        return searchResponse
    }

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
                'jummpPropFile': configurationService.getConfigFilePath(),
                'miriamExportFile': registryExport,
                'searchStrategy': searchStrategy,
                'database': dbSettings)
            File indexingData = new File(exchangeFolder, "indexData.json")
            indexingData.setText(builder.toPrettyString())

            String jarPath = grailsApplication.config.jummp.search.pathToIndexerExecutable
            def argsMap = [jarPath: jarPath, jsonPath: indexingData.getCanonicalPath()]

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
                producerTemplate.sendBody("seda:exec", argsMap)
            } catch (Exception e) {
                log.error("Failed to index revision $revision.properties - ${e.message}", e)
                //TODO RETRY
            }
        }
    }

    private List<String> fetchFilesFromRevision(RevisionTransportCommand rev, boolean filterMains) {
        if (filterMains) {
            return rev?.files?.findAll{it.mainFile}.collect{it.path}
        }
        return rev?.files?.collect{it.path}
    }
}
