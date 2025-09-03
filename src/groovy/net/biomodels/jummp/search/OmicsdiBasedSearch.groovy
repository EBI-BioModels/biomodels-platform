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

import grails.transaction.NotTransactional
import grails.util.Environment
import grails.util.Holders
import groovy.json.JsonBuilder
import net.biomodels.jummp.annotationstore.ElementAnnotation
import net.biomodels.jummp.annotationstore.ResourceReference
import net.biomodels.jummp.annotationstore.RevisionAnnotation
import net.biomodels.jummp.annotationstore.Statement
import net.biomodels.jummp.core.ModelSearchStrategy as MST
import net.biomodels.jummp.core.events.ModelOperationEvent
import net.biomodels.jummp.core.model.ModelFormatTransportCommand
import net.biomodels.jummp.core.model.ModelState
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.PublicationTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand as RevisionTC
import net.biomodels.jummp.core.model.identifier.ModelIdentifierUtils
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.utils.EbiSearchHelper
import net.biomodels.jummp.utils.FileHelper
import net.biomodels.jummp.utils.WebServiceFetcher
import org.codehaus.groovy.grails.plugins.support.aware.GrailsConfigurationAware as GCA
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import uk.ac.ebi.ddi.ebe.ws.dao.client.dataset.DatasetWsClient
import uk.ac.ebi.ddi.ebe.ws.dao.config.AbstractEbeyeWsConfig
import uk.ac.ebi.ddi.ebe.ws.dao.config.EbeyeWsConfigDev
import uk.ac.ebi.ddi.ebe.ws.dao.config.EbeyeWsConfigProd
import uk.ac.ebi.ddi.ebe.ws.dao.model.common.Entry
import uk.ac.ebi.ddi.ebe.ws.dao.model.common.Facet
import uk.ac.ebi.ddi.ebe.ws.dao.model.common.FacetValue
import uk.ac.ebi.ddi.ebe.ws.dao.model.common.QueryResult

import java.text.SimpleDateFormat
import java.util.regex.Pattern

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

class OmicsdiBasedSearch implements GCA, MST, ApplicationListener<ModelOperationEvent> {
    /**
     * The class logger.
     */
    static final Logger LOGGER = LoggerFactory.getLogger(OmicsdiBasedSearch.class)
    /**
     * Flag indicating the logger's verbosity threshold.
     */
    static final boolean IS_DEBUG_ENABLED = LOGGER.isDebugEnabled()
    /**
     * Flag indicating the logger's verbosity threshold.
     */
    static final boolean IS_INFO_ENABLED = LOGGER.isInfoEnabled()
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd")

    private final Pattern pattern = ~/(\p{Alnum}+:)(\p{Alnum}+):(\d+)/
    private final String replacement = '$1$2\\\\:$3' // note the single quotes to avoid Groovy string interpolation

    private final Map<String, Integer> FACET_ORDER = new TreeMap<String, Integer>(String.CASE_INSENSITIVE_ORDER) {
        {
            put("Curation status", 1)
            put("Model format", 2)
            put("Modelling approach", 3)
            put("Model Tag", 4)
            put("Organisms", 5)
            put("Disease", 6)
            put("GO", 7)
            put("UniProt", 8)
            put("ChEBI", 9)
            put("ChEMBL", 10)
            put("Ensembl", 11)
            put("Model flag", 12)
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

    def modelTagService = Holders.grailsApplication.mainContext.getBean('modelTagService')

    def producerTemplate = Holders.grailsApplication.mainContext.getBean('producerTemplate')

    private String httpProxyHost = "localhost"
    private int httpProxyPort = 80

    @Override
    void setConfiguration(ConfigObject co) {
        httpProxyHost = co.jummp.http.proxy.host
        httpProxyPort = co.jummp.http.proxy.port as int
    }

    @NotTransactional
    void onApplicationEvent(ModelOperationEvent event) {
        // look at solrbasedsearch
    }

    @NotTransactional
    String[] getSortFields() {
        ["relevance", "submissionid", "name"]
    }

    @Override
    @NotTransactional
    Map checkIndexedData() {
        LOGGER.debug("Checking whether BioModels duplicated entries on EBI Search Server...")
        EbiSearchHelper.checkIndexedData()
    }

    @Override
    void indexDB() {
        // Export OmicsDI XML Files and leave them processed by DDI Team and EBI Search Team
        // This job will call a service running on the ebi-mol-sys-dev machine to launch a job on SLURM cluster
        // It will generate all OmicsDI XML files.
        boolean inProdMode = Environment.current == Environment.PRODUCTION
        inProdMode = true // test it before committing
        if (inProdMode) {
            LOGGER.info("Submitted the job for exporting OmicsDI XML files...")
            final String SVC_URL = "http://ebi-mol-sys-dev.ebi.ac.uk:8000/search/export/omicsdi"
            WebServiceFetcher fetcher = new WebServiceFetcher(SVC_URL)
            def result = fetcher.getText()
            LOGGER.info(result as String)
        } else {
            LOGGER.info("No need to export OmicsDI XML files for the local development.")
        }
    }

    @NotTransactional
    SearchResponse searchModels(String query, String domain, SortOrder sortOrder,
            Map<String, Integer> paginationCriteria = ["start": 0, "length": 50, "facetCount": 20] ) {
        long startAt = System.currentTimeMillis()
        boolean inProdMode = Environment.current == Environment.PRODUCTION
        AbstractEbeyeWsConfig ebeyeWsConfig
        if (inProdMode) {
            ebeyeWsConfig = new EbeyeWsConfigProd()
        } else {
            ebeyeWsConfig = new EbeyeWsConfigDev()
        }
        /**
         * At the current settings, using HTTP PROXY to access the internet in the cloud based deployment.
         * Because of that, we need to tell ddi-ebe-ws-dao the information of HTTP PROXY
         */
        String _httpProxyHost = System.getProperty("http.proxyHost") ?: System.getenv("http.proxyHost")
        String proxyHost = _httpProxyHost != null ? _httpProxyHost : this.httpProxyHost
        String _httpProxyPort = System.getProperty("http.proxyPort") ?: System.getenv("http.proxyPort")
        int proxyPort = _httpProxyPort != null ? _httpProxyPort.toInteger().intValue() : this.httpProxyPort
        ebeyeWsConfig.setHttpProxyHost(proxyHost)
        ebeyeWsConfig.setHttpProxyPort(proxyPort)
        DatasetWsClient datasetWsClient = new DatasetWsClient(ebeyeWsConfig)
        // parse raw query to OmicsDI API to avoid double encoding issues.
        final String rawQuery = query.decodeHTML()
        // escape special Lucene field separators in query string
        query = escapeLuceneFieldSeparator(rawQuery)
        // TODO: should allow searching information of other fields
        // create the returned object
        SearchResponse searchResponse = new SearchResponse()
        String[] fields = ["name", "description", "submitter", "curationstatus",
                           "last_modification_date", "submission_date", "modellingapproach",
                           "modelformat", "levelversion", "first_author", "publication_year", "isprivate", "submitter_keywords"]
        String sortField = sortOrder.getField()
        String sortDir = sortOrder.direction == SortOrder.SortDirection.ASC ? "ascending" : "descending"
        String sort = sortField ? String.format("%s:%s", sortField, sortDir) : ""
        /* By default, we put the private models at the last pages if they are available */
        sort = sort ? "isprivate:ascending,$sort" : "isprivate:ascending"
        QueryResult result
        try {
            int start = paginationCriteria['start']
            int length = paginationCriteria['length']
            int facetCount = paginationCriteria['facetCount']
            result = datasetWsClient.getDatasets(domain, query, fields, start, length, facetCount, sort)
        } catch (HttpServerErrorException e) {
            LOGGER.debug("""There was a problem obtaining search result from EBI search server. \
The root cause is ${e.toString()}""")
            LOGGER.debug("Status code: ${e.statusCode.value()}. Message: ${e.message}")
            result = null
        } catch (HttpClientErrorException e) {
            LOGGER.error("There was a problem searching models from BioModels ${e.toString()}")
            if (e.statusCode.value() == 400) {
                LOGGER.error("The querying string might be wrong syntax or contains restricted characters.")
            }
            result = null
        } catch (UnknownHostException ignored ) {
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
                println entry.dump()
                LOGGER.info(entry.dump())
                ModelTransportCommand mtc
                String submissionId = entry.id
                String modelName = getSingleValueForEntryField(entry, 'name')
                String description = getSingleValueForEntryField(entry, 'description')

                boolean haveSubmissionDate = getValueArrayForEntryField(entry, 'submission_date').length > 0
                boolean haveModifiedDate = getValueArrayForEntryField(entry, 'last_modification_date').length > 0
                boolean haveSubmitter = getValueArrayForEntryField(entry, 'submitter').length > 0
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
                    String submissionDateString = getSingleValueForEntryField(entry, 'submission_date')
                    Date submissionDate = null
                    try {
                        if (submissionDateString != "") {
                            submissionDate = formatParsedDateString(submissionDateString)
                        }
                    } catch (NumberFormatException e1) {
                        println("${submissionId} because of ${e1.message} - ${entry.dump()}")
                        LOGGER.debug("${submissionId} because of ${e1.message} - ${entry.dump()}")
                    }
                    String submitterName = getSingleValueForEntryField(entry, 'submitter')
                    String modifiedDateString = getSingleValueForEntryField(entry,
                            'last_modification_date')
                    Date modifiedDate = null
                    try {
                        if (modifiedDateString != "") {
                            modifiedDate = formatParsedDateString(modifiedDateString)
                        }
                    } catch (NumberFormatException e1) {
                        println("${submissionId} because of ${e1.message} - ${entry.dump()}")
                        LOGGER.debug("${submissionId} because of ${e1.message} - ${entry.dump()}")
                    }
                    String formatName = getSingleValueForEntryField(entry, 'modelformat')
                    String formatVersion = getSingleValueForEntryField(entry, 'levelversion')
                    String publicationYear = getSingleValueForEntryField(entry, 'publication_year')
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
                if (facet.id == "submitter_keywords") {
                    facet.label = "Model Tag"
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
            LOGGER.debug("Search terms: $query")
            LOGGER.debug("Results processed in ${System.currentTimeMillis() - startAt}")
        }
        return searchResponse
    }

    void updateIndex(RevisionTC revision,
                     Map<String, String> options = ["level": "full", "indexer": ""] as Map) {
        LOGGER.info("Indexing options for this revsion ${revision.identifier()}: $options")
        Revision.withSession {
            def partialData = buildPartialData(revision)
            String exchangeFolder = grailsApplication.config.jummp.vcs.exchangeDirectory
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
            builder(partialData: partialData,
                'folder': exchangeFolder,
                'mainFiles': fetchFilesFromRevision(revision, true),
                'allFiles': fetchFilesFromRevision(revision, false),
                'jummpPropFile': configurationService.getConfigFilePath(),
                'miriamExportFile': registryExport,
                'searchStrategy': searchStrategy,
                'database': dbSettings,
                'level': options["level"],
                'indexer': options["indexer"])
            String sep = File.separator

            String indexingFolder = "$exchangeFolder${sep}indexing${sep}${revision.identifier()}"
            File indexingData = FileHelper.createFile(indexingFolder, "indexData.json")
            if (indexingData) {
                indexingData.setText(builder.toPrettyString())
            } else {
                LOGGER.info("Cannot create indexData.json to index the model {}.", revision.identifier())
                throw new RuntimeException("Cannot create indexData.json to index the model ${revision.identifier()}.")
            }

            String jarPath = grailsApplication.config.jummp.search.pathToIndexerExecutable
            def argsMap = [jarPath: jarPath, jsonPath: indexingData.absolutePath]
            argsMap.putAll(configurationService.configureProxySettings() as Map<? extends String, ? extends String>)

            try {
                producerTemplate.sendBody("seda:exec", argsMap)
            } catch (Exception e) {
                LOGGER.error("Failed to index revision $revision.properties - ${e.message}", e)
                //TODO RETRY
            }
        }
    }

    void clearIndex() {
        // Delete indexing plans from the database
        if (IS_DEBUG_ENABLED) {
            LOGGER.debug "Clearing the indexing plans."
        }
        Revision.executeUpdate("delete IndexingPlan")
    }

    void clearIndex(RevisionTC revisionTC) {
        List revisionAnnotationRecords = RevisionAnnotation.findAll {
            revision.id == revisionTC.id
        }

        // delete RevisionAnnotation
        List statements = new ArrayList<>()
        revisionAnnotationRecords.each {
            statements.add(it.elementAnnotation.statement)
            it.delete(flush: true)
        }

        // delete ElementAnnotation
        // it's unnecessary
        // List listElementAnnotation = revisionAnnotationRecords*.elementAnnotation
        /*listElementAnnotation.each {
            it.delete(flush: true)
        }*/

        // delete Statement and ResourceReference
        statements.each { def stmt ->
            List rs = ElementAnnotation.findAllByStatement(stmt as Statement)
            if (!rs?.size()) {
                ResourceReference rr = stmt.object as ResourceReference
                Set stmts = rr.statements
                if (stmts?.size() == 1) {
                    rr.delete(flush: true)
                } else {
                    Set updatedStmts = stmts.findAll { s ->
                        s.id != stmt.id
                    }
                    rr.statements = updatedStmts
                    rr.save(flush: true)

                    stmt.delete(flush: true)

                    if (!rr.statements?.size()) {
                        rr.delete(flush: true)
                    }
                }
            }
        }
    }

    private Date formatParsedDateString(String dateString) {
        Date date = null
        if (!dateString.isEmpty()) {
            date = dateFormat.parse(dateString)
        }
        date
    }

    private String getSingleValueForEntryField(Entry entry, String field) {
        String[] values = getValueArrayForEntryField(entry, field)
        if (values.length > 0) {
            return values[0]
        }
        ""
    }

    private String[] getValueArrayForEntryField(Entry entry, String field) {
        Objects.requireNonNull(entry)
        final String[] defaultResult = new String[0]
        String[] values = entry.getFields().get(field)
        if (!values) {
            return defaultResult // save client from testing for null
        }
        values
    }

    private List<String> fetchFilesFromRevision(RevisionTC rev, boolean filterMains) {
        if (filterMains) {
            return rev?.files?.findAll{it.mainFile}.collect{it.path}
        }
        return rev?.files?.collect{it.path}
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

    private Map buildPartialData(RevisionTC revision) {
        String submissionId = revision.model.submissionId
        String publicationId = revision.model.publicationId ?: ""
        String name = revision.name ?: ""
        String description = revision.description ?: ""

        int versionNumber = revision.revisionNumber
        boolean isCertified = null != revision.qcInfo

        final String uniqueId = "${submissionId}.${versionNumber}"
        def tags = modelTagService.getTagsByModelId(revision?.model?.submissionId)

        Map data = [
            'submissionId': submissionId,
            'publicationId' :publicationId,
            'name': name,
            'description' : description,
            'modelFormat' : revision.format.identifier,
            'levelVersion' : revision.format.formatVersion,
            'submitter' : revision.owner,
            'submitterUsername' :  revision.model.submitterUsername,
            'publicationLinkType': revision.model.publication ? revision.model.publication?.linkProvider.linkType : "",
            'publicationLink': revision.model.publication ? revision.model.publication.link : "",
            'publicationTitle' : revision.model.publication ?
                revision.model.publication.title  :  "",
            'publicationAbstract' : revision.model.publication ?
                revision.model.publication.synopsis : "",
            'publicationAuthor': revision.model.publication?.authors ?
                revision.model.publication.authors.collect {
                    it.userRealName }.join(', ') : "",
            'publicationYear': revision.model.publication?.year ?: 0,
            'modelId' : revision.model.id,
            'revisionId' :  revision.id,
            'deleted' :  revision.model.deleted,
            'public' :  revision.model.firstPublished ? 'true'  :  'false',
            'certified' : isCertified ? 'true' : 'false',
            'versionNumber' : versionNumber,
            'submissionDate' : revision.model.submissionDate,
            'lastModified' :  revision.model.lastModifiedDate,
            'uniqueId' : uniqueId,
            'tags': tags,
            'isMetadataSubmission': revision.model.isMetadataSubmission
        ]
        return data
    }
}
