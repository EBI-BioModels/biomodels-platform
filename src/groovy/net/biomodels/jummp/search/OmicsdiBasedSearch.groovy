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

import grails.util.Environment
import grails.util.Holders
import groovy.json.JsonBuilder
import net.biomodels.jummp.annotationstore.ResourceReference
import net.biomodels.jummp.core.ModelSearchStrategy
import net.biomodels.jummp.core.events.ModelOperationEvent
import net.biomodels.jummp.core.model.ModelFormatTransportCommand
import net.biomodels.jummp.core.model.ModelState
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.PublicationTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.core.model.identifier.ModelIdentifierUtils
import net.biomodels.jummp.model.Revision
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.context.ApplicationListener
import org.springframework.web.client.HttpClientErrorException
import uk.ac.ebi.ddi.ebe.ws.dao.client.dataset.DatasetWsClient
import uk.ac.ebi.ddi.ebe.ws.dao.config.AbstractEbeyeWsConfig
import uk.ac.ebi.ddi.ebe.ws.dao.config.EbeyeWsConfigDev
import uk.ac.ebi.ddi.ebe.ws.dao.config.EbeyeWsConfigProd
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

    private final java.util.regex.Pattern pattern = ~/(\p{Alnum}+:)(\p{Alnum}+):(\d+)/
    private final String replacement = '$1$2\\\\:$3' // note the single quotes to avoid Groovy string interpolation

    private final Map<String, Integer> FACET_ORDER = new TreeMap<String, Integer>(String.CASE_INSENSITIVE_ORDER) {
        {
            put("Curation status", 1)
            put("Model format", 2)
            put("Modelling approach", 3)
            put("Model flag", 4)
            put("Organisms", 5)
            put("Disease", 6)
            put("GO", 7)
            put("UniProt", 8)
            put("ChEBI", 9)
            put("ChEMBL", 10)
            put("Ensembl", 11)
        }
    }

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

    private String escapeLuceneFieldSeparator(String query) {
        def matcher = query =~ pattern
        def out = new StringBuffer()
        while (matcher) {
            matcher.appendReplacement(out, replacement)
        }
        matcher.appendTail(out)
        out.toString()
    }

    SearchResponse searchModels(String query, SortOrder sortOrder,
            Map<String, Integer> paginationCriteria = ["start": 0, "length": 50, "facetCount": 10] ) {
        long start = System.currentTimeMillis()
        boolean inDevMode = Environment.isDevelopmentMode()
        AbstractEbeyeWsConfig ebeyeWsConfig
        if (inDevMode) {
            ebeyeWsConfig = new EbeyeWsConfigDev()
        } else {
            ebeyeWsConfig = new EbeyeWsConfigProd()
        }
        DatasetWsClient datasetWsClient = new DatasetWsClient(ebeyeWsConfig)
        // escape special Lucene field separators in query string
        query = escapeLuceneFieldSeparator(query)
        // TODO: should allow searching information of other fields
        // create the returned object
        SearchResponse searchResponse = new SearchResponse()
        String[] fields = ["name", "description", "submitter", "curationstatus",
                           "last_modification_date", "submission_date",
                           "modelformat", "levelversion", "first_author", "publication_year", "isprivate"]
        String sortField = sortOrder.getField()
        String sortDir = sortOrder.direction == SortOrder.SortDirection.ASC ? "ascending" : "descending"
        String sort = sortField ? String.format("%s:%s", sortField, sortDir) : ""
        /* By default, we put the private models at the last pages if they are available */
        sort = sort ? "isprivate:ascending,$sort" : "isprivate:ascending"
        QueryResult result
        try {
            result = datasetWsClient.getDatasets("biomodels", query, fields,
                paginationCriteria['start'], paginationCriteria['length'], paginationCriteria['facetCount'], sort)
        } catch (HttpClientErrorException e) {
            log.debug("""\
There was a problem obtaining search result from EBI search server. The root cause is ${e.toString()}""")
            log.debug("Status code: ${e.statusCode.value()}. Message: ${e.message}")
            if (e.statusCode.value() == 400) {
                log.debug("The querying string might be wrong syntax or contains restricted characters.")
            }
            result = null
        }
        List<Facet> facets = []
        int totalCount
        // convert all the returned entries to ModelTransportCommand objects
        List<ModelTransportCommand> results = new ArrayList<ModelTransportCommand>()
        if (result) {
            // entries/models
            totalCount = result.count
            List<Entry> entries = result.getEntries()
            entries?.eachWithIndex { Entry entry, int i ->
                ModelTransportCommand mtc
                String submissionId = entry.id
                String modelName = entry.getFields().get('name')[0]
                String description = ""
                boolean haveDescription = entry.getFields().get('description')?.length > 0
                if (haveDescription) {
                    description = entry.getFields().get('description')[0]
                }
                boolean haveSubmissionDate = entry.getFields().get('submission_date')?.length > 0
                boolean haveModifiedDate = entry.getFields().get('last_modification_date')?.length > 0
                boolean haveSubmitter = entry.getFields().get('submitter')?.length > 0
                ModelState state
                if (!haveSubmissionDate && !haveModifiedDate && !haveSubmitter) {
                    // TODO: make the condition of a private model stronger
                    state = ModelState.UNPUBLISHED
                    mtc = new ModelTransportCommand(
                        submissionId: submissionId,
                        name: modelName,
                        description: description,
                        state: state
                    )
                } else {
                    state = ModelState.PUBLISHED
                    String submissionDateString = entry.getFields().get('submission_date')[0]
                    java.text.SimpleDateFormat simpleDateFormat = new java.text.SimpleDateFormat("yyyyMMdd")
                    Date submissionDate = simpleDateFormat.parse(submissionDateString)
                    String submitterName = entry.getFields().get('submitter')[0]
                    String modifiedDateString = entry.getFields().get('last_modification_date')[0]
                    simpleDateFormat = new java.text.SimpleDateFormat("yyyyMMdd")
                    Date modifiedDate = simpleDateFormat.parse(modifiedDateString)
                    String formatName = entry.getFields().get('modelformat')[0]
                    String formatVersion = entry.getFields().get('levelversion')[0]
                    boolean havePublicationYear = entry.getFields().get('publication_year')?.length > 0
                    String publicationYear = ""
                    if (havePublicationYear) {
                        publicationYear = entry.getFields().get('publication_year')[0]
                    }
                    ModelFormatTransportCommand format =
                        new ModelFormatTransportCommand(name: formatName, formatVersion: formatVersion)
                    PublicationTransportCommand ptc = null
                    if (publicationYear) {
                        ptc = new PublicationTransportCommand(year: Integer.parseInt(publicationYear))
                    }
                    mtc = new ModelTransportCommand(
                        submitter: submitterName,
                        name: modelName,
                        description: description,
                        submissionId: submissionId,
                        submissionDate: submissionDate,
                        lastModifiedDate: modifiedDate,
                        state: state,
                        format: format,
                        publication: ptc
                    )
                }
                results.add(mtc)
            }
            // facets
            Set<String> hiddenFacets = ["PUBLICATION DATE", "OMICS TYPE", "REPOSITORY", "SOURCE", "ISPRIVATE"]
            boolean shouldBeHidden = false
            result.facets?.each { Facet facet ->
                // deal with two fields due to camel case in the field names
                // will be cleaned up once www-prod team launches the next configuration
                if (facet.id == "modellingApproach") {
                    facet.id = "modellingapproach"
                }
                if (facet.id == "modelFlag") {
                    facet.id = "modelflag"
                }

                shouldBeHidden = hiddenFacets.contains(facet.label.toUpperCase())
                if (!shouldBeHidden) {
                    facets.add(facet)
                }
            }
        } else {
            totalCount = 0
            results = []
            facets = []
        }
        Set<String> immutableFacets = ["Organisms", "Publication Date", "Omics type"]

        // potentially turn facets into a HashSet/HashMap so that we can more easily
        // compute the delta b/w facets and immutable facets
        List<FacetValue> facetValues = facets.findAll({ Facet f ->
            !(immutableFacets.contains(f.label))
        })*.facetValues.flatten().findAll { FacetValue value -> !value.label.contains(' ') }
        Map<String, String> labelsForAccessions = new LinkedHashMap<>(facetValues.size())
        List<String> labels = facetValues.collect { it.label }
        List<ResourceReference> references = ResourceReference.findAllByAccessionInList(labels)
        references.each { ResourceReference r ->
            labelsForAccessions[r.accession] = r.name
        }
        facetValues.each { FacetValue v ->
            String referenceName = labelsForAccessions[v.label]
            if (referenceName) {
                v.label = referenceName
            }
        }
        // build a TreeMap based on the deliberately designed order of our Facets
        TreeSet<OrderedFacet> orderedFacets = new TreeSet<OrderedFacet>()
        facets.each {Facet facet ->
            int order = FACET_ORDER.get(facet.label) ?: FACET_ORDER.size() + 1
            OrderedFacet of = new OrderedFacet(facet, order)
            orderedFacets.add(of)
        }
        // do not care about the facets order because an LinkedHashMap object can preserve
        // the insertion order. Here we just copy all facets ordered above to the
        // SearchResponse's facets placeholder
        orderedFacets.each {
            searchResponse.facets.putAt(it.facet.label, it)
        }
        searchResponse.results = results
        searchResponse.totalCount = totalCount
        if (IS_DEBUG_ENABLED) {
            log.debug("Search terms: $query")
            log.debug("Results processed in ${System.currentTimeMillis() - start}")
        }
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
                'jummpPropFile': configurationService.getConfigFilePath(),
                'miriamExportFile': registryExport,
                'searchStrategy': searchStrategy,
                'database': dbSettings)
            File indexingData = new File(exchangeFolder, "indexData.json")
            indexingData.setText(builder.toPrettyString())

            String jarPath = grailsApplication.config.jummp.search.pathToIndexerExecutable
            def argsMap = [jarPath: jarPath, jsonPath: indexingData.absolutePath]

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

    String[] getSortFields() {
        ["relevance", "submissionid", "name"]
    }

    private List<String> fetchFilesFromRevision(RevisionTransportCommand rev, boolean filterMains) {
        if (filterMains) {
            return rev?.files?.findAll{it.mainFile}.collect{it.path}
        }
        return rev?.files?.collect{it.path}
    }
}
