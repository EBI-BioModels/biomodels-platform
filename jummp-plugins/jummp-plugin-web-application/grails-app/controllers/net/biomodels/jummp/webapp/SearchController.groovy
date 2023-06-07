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
*
* Additional permission under GNU Affero GPL version 3 section 7
*
* If you modify Jummp, or any covered work, by linking or combining it with
* Grails, Spring Security (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Grails, Spring Security used as well as
* that of the covered work.}
**/

package net.biomodels.jummp.webapp

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.plugin.springsecurity.authentication.GrailsAnonymousAuthenticationToken
import groovy.json.JsonBuilder
import net.biomodels.jummp.core.adapters.ModelAdapter
import net.biomodels.jummp.core.model.ModelListSorting
import net.biomodels.jummp.core.model.ModelTransportCommand as MTC
import net.biomodels.jummp.core.model.RevisionTransportCommand as RTC
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.search.OrderedFacet
import net.biomodels.jummp.search.SearchResponse
import net.biomodels.jummp.search.SortOrder
import net.biomodels.jummp.utils.redis.KeyCollection
import net.biomodels.jummp.webapp.rest.search.BrowseResults
import net.biomodels.jummp.webapp.rest.search.SearchResults
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.ac.ebi.ddi.ebe.ws.dao.model.common.Facet

@Secured(['IS_AUTHENTICATED_FULLY'])
class SearchController {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass())
    /**
     * Dependency Injection of Spring Security Service
     */
    def springSecurityService
    /**
     * Dependency injection of searchService.
     */
    def searchService
    /**
     * Dependency injection of modelService.
     **/
    def modelService

    def modelDelegateService
    /**
     * Dependency injection of modelHistoryService.
    **/
    def modelHistoryService

    def publishClientService

    def index = {
        redirect action: 'search'
    }

    private boolean integerCheck(def input, boolean minValueCheck=false, int minValue=-1) {
        try {
            if (!input) {
                return false
            }
            int value = Integer.parseInt(input)
            if (minValueCheck) {
                return value > minValue
            }
            return true
        }
        catch(Exception e) {
            return false
        }
    }

    private void sanitiseParams() {
        // the statements below only perform an extraction and analyse parameters
        // the sanitization of the parameters was performed earlier in ParameterFilters
        if (params.sort) {
            def sortVal = params.sort.split("-")
            params.sortBy = sortVal[0]
            params.sortDir = sortVal[1]
        } else {
            params.sortBy = "relevance"
            params.sortDir = "desc"
        }
        params.numResults = numResults()
        if (integerCheck(params.offset, true, -1)) {
            params.offset = params.offset ? params.int("offset") : 0
        }
        else {
            params.offset = 0
        }
    }

    private int numResults() {
        final int MAXRESULTS = 100
        final int MINRESULTS = 10
        User user
        String username = springSecurityService?.principal?.username
        if (!(username == GrailsAnonymousAuthenticationToken.USERNAME) && !username) {
            user = User.findByUsername(username)
        }
        Preferences prefs
        if (user) {
            prefs = Preferences.findByUser(user)
        }
        if (!prefs) {
            prefs = Preferences.getDefaults()
        }
        if (integerCheck(params.numResults, true, -1)) {
            prefs.numResults = params.int("numResults")
            if (prefs.numResults > MAXRESULTS ) {
                prefs.numResults = MAXRESULTS
            }
            else if (prefs.numResults < MINRESULTS ) {
                prefs.numResults = MINRESULTS
            }
            if (user) {
                prefs.setUser(user)
                prefs.save(flush: true)
            }
        }
        return prefs.numResults
    }

    /**
     * Default action showing a list view
     */
    @Secured(['IS_AUTHENTICATED_FULLY'])
    def list() {
        sanitiseParams()
        def results = browseCore(params.sortBy as String,
            params.sortDir as String, params.offset as int,
            params.numResults as int, params.query as String)

        if (response.format == "html") {
            results["history"] = modelHistoryService.history()
            return results
        }
        respond new BrowseResults(results)
    }

    /**
     * Default action showing a archive view
     */
    @Secured(['IS_AUTHENTICATED_FULLY'])
    def archive() {
        sanitiseParams()
        def results = archiveCore(params.sortBy, params.sortDir, params.offset, params.numResults)
        return results
    }

    @Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
    def searchRedir() {
        // avoid encoding as HTML twice
        String query = params.search_block_form.decodeHTML()
        redirect action: 'search', params: [query: query, domain: params.chosenDomain]
    }

    /**
     * Default action showing a list view
     */
    @Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
    def search() {
//        publishClientService.publish(KeyCollection.REDIS_CHANNEL_MODEL_ID_LAST_USED_VALUE, "MODEL1234")
        sanitiseParams()
        if (!params.query) {
            params.query = ""
        } else {
            if (params.query in ["*", "*.*", "*?*", "***"]) {
                params.query = "*:*"
                params.flashMessage = "Please use *:* to browse all models."
            }
        }
        if (!params.domain) {
            params.domain = "biomodels"
        }
        /**
         * Check whether the request isn't re-processed by Load Balancer
         */
        String clientIPAddress = request.getHeader("X-Forwarded-For") ?: request.getRemoteAddr()
        String searchInfo = """Search terms: ${params.query}, requested from: ${clientIPAddress} \
under the format: ${response.format}"""
        LOGGER.debug(searchInfo)
        Map results = searchCore(params.query as String,
            params.domain as String, params.sortBy as String,
            params.sortDir as String, params.offset as int, params.numResults as int)
        if (response.format == "html") {
            return results
        }
        respond new SearchResults(results)
    }

    @Secured(['ROLE_ADMIN'])
    def regen() {
        render(view: "regen")
    }

    @Secured(['ROLE_ADMIN', 'ROLE_CURATOR'])
    def reindex() {
        def models = params.models.split(",")
        Map<String, String> msgMap = [:]
        models.each { String model ->
            String message = ""
            model = model.trim()
            if (model == null) {
                // show reindex view to provide an interface for users
                // where entering model revision
                message = "Please enter the identifiers of the models you want to re-index"
            } else  {
                // show reindex view and
                // display the successful message about reindexing the model revision
                RTC revision = null
                try {
                    revision = modelDelegateService.getRevisionFromParams(model)
                } catch (org.springframework.security.access.AccessDeniedException ade) {
                    message = "Unable to access the model"
                }
                if (revision) {
                    model = revision.identifier()
                    searchService.updateIndex(revision)
                    message = "Started re-indexing the model"
                }
            }
            String modelLink = createLink(controller: "model", action: "show", id: model, absolute: true)
            msgMap[modelLink] = message
        }
        [msgMap: msgMap]
    }

    @Secured(['ROLE_ADMIN'])
    def regenIndices() {
        long start = System.currentTimeMillis()
        searchService.regenerateIndices()
        def regenTime = System.currentTimeMillis() - start
        def result ="""\
<h3>Report</h3>
<p>Indices regenerated!<br/>Regenerated in ${regenTime/1000f}ms</p>"""
        render result
    }

    @Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
    def download() {
        if (!params.models) {
            def params = [query: "*:*", flashMessage : g.message(code: "jummp.search.download.warningMessage")]
            forward action: 'search', params: params
            return // don't continue any further with this.
        }
        String[] models = params.models?.split(',')
        if (models?.size() > 100) {
            def params = [query: "*:*",
                          flashMessage: g.message(code: "jummp.search.download.exceededThreshold.warningMessage")]
            forward(action: 'search', params: params)
            return params
        }
        def data = modelDelegateService.serveModelFilesAsZip(models)
        try {
            if (data) {
                // the data could be null in a few situations such as the model files are inaccessible
                response.setContentType("application/zip")
                String date = new Date().format("yyyyMMdd-HHmm")
                String filename = "BioModels-search-results_${date}.zip".toString()
                response.setHeader("Content-disposition", "attachment;filename=\"${filename}\"")
                response.outputStream << data
                response.outputStream.flush()
            } else {
                render(view: "download", status: 404)
            }
        } catch (Exception exception) {
            LOGGER.error("Exception on downloading search result:", exception)
        } finally {
            LOGGER.debug("Number of models have been downloaded: ${models.join(", ")}")
            data?.close()
            if (response.outputStream){
                try {
                    response.outputStream.close()
                } catch (IOException ioe) {
                    response.reset()
                    LOGGER.error("Exception on closing the output stream of the response object:", ioe)
                }
            }
        }
    }

    private Map searchCore(String query, String domain, String sortBy,
                           String sortDirection, int offset, int length) {
        Map<String, Integer> paginationCriteria = ["start": offset, "length": length, "facetCount": 1000]
        SortOrder sortOrder = new SortOrder(sortBy, sortDirection)
        List<MTC> models = []
        List<Facet> facets = []
        int totalCount
        if (query?.trim()) {
            SearchResponse response = searchService.searchModels(query, domain, sortOrder, paginationCriteria)
            ArrayList<MTC> res = response.results
            totalCount = response.totalCount
            if (res?.size() > 0) {
                res.each {
                    models.add(it)
                }
            }
            LinkedHashMap<String, OrderedFacet> respondedFacets = response.facets
            if (respondedFacets?.size() > 0) {
                respondedFacets.each {
                    facets.add(it.value.facet)
                }
            }
            LOGGER.info("Found: ${res?.size() ?: 0} records, ${respondedFacets?.size() ?: 0} facets.")
        }
        JsonBuilder builder = new JsonBuilder(facets)

        if (offset > 0 && offset < models?.size()) {
            models = models[offset..-1]
        } else {
            offset = 0
        }
        if (models?.size() > length) {
            models = models[0..length-1]
        }

        /**
         * By default, the query was encoded as HTML due to security vulnerability until here.
         * After using encoded query into search modules, we should decode it into the original
         * value that helps displaying it in a human readable form. Pay attention to the fact that
         * the query has been decoded in searchService.searchModels.
         */
        query = query.decodeHTML()
        return [models: models, facets: facets, matches: totalCount,
                offset: paginationCriteria['start'],
                length: paginationCriteria['length'],
                sortBy: sortBy, sortDirection: sortDirection,
                query: query, facetStats: builder.toString()]
    }

    private def archiveCore(String sortBy, String sortDirection, int offset, int length) {
        ModelListSorting sort = inferSortedColumn(sortBy)
        List modelsDomain =
                modelService.getAllModels(offset, length, sortDirection == "asc", sort, null, true)
        List models = []
        modelsDomain.each {
            models.add(new ModelAdapter(model: it).toCommandObject())
        }
        return [models: models, modelsAvailable: modelService.getModelCount(null, true),
                sortBy: sortBy, sortDirection: sortDirection, offset: offset, length: length]
    }

    private def browseCore(String sortBy, String sortDirection, int offset, int length, String filter) {
        ModelListSorting sort = inferSortedColumn(sortBy)
        List modelsDomain = modelService.getMyModels(offset, length, sortDirection == "asc", sort, filter)
        List models = []
        modelsDomain.each {
            MTC m = new ModelAdapter(model: it).toCommandObject(false)
            models.add(m)
        }
        List<Facet> basicFacets = searchService.buildBasicFacets(models)
        int totalCount = modelService.countMyModels(filter, false)
        return [models: models, facets: basicFacets, modelsAvailable: totalCount, sortBy: sortBy,
                sortDirection: sortDirection, offset: offset, length: length, query: filter]
    }

    private ModelListSorting inferSortedColumn(final String sortBy) {
        ModelListSorting sort
        switch (sortBy) {
            case "name":
                sort = ModelListSorting.NAME
                break
            case "format":
                sort = ModelListSorting.FORMAT
                break
            case "submitter":
                sort = ModelListSorting.SUBMITTER
                break
            case "submitted":
                sort = ModelListSorting.SUBMISSION_DATE
                break
            case "modified":
                sort = ModelListSorting.LAST_MODIFIED
                break
            default:
                sort = ModelListSorting.ID
                break
        }
        sort
    }

    def lastAccessedModels = {
        List data = modelHistoryService.history()
        def dataToRender = []
        data.each { model ->
            dataToRender << [id: model.id, name: model.name, submitter: model.submitter]
        }
        render dataToRender as JSON
    }
}
