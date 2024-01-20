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





package net.biomodels.jummp.core

import grails.async.Promise
import grails.plugin.springsecurity.annotation.Secured
import groovy.json.JsonBuilder
import net.biomodels.jummp.core.adapters.RevisionAdapter
import net.biomodels.jummp.core.events.LoggingEventType
import net.biomodels.jummp.core.events.PostLogging
import net.biomodels.jummp.core.model.ModelState
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.ModelTransportCommand as ModelTC
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.search.OmicsdiBasedSearch
import net.biomodels.jummp.search.OrderedFacet
import net.biomodels.jummp.search.SearchResponse
import net.biomodels.jummp.search.SolrBasedSearch
import net.biomodels.jummp.search.SortOrder
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.perf4j.aop.Profiled
import org.springframework.beans.factory.InitializingBean
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import uk.ac.ebi.ddi.ebe.ws.dao.model.common.Facet
import uk.ac.ebi.ddi.ebe.ws.dao.model.common.FacetValue

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * @short Singleton-scoped facade for interacting with a searching service's instance.
 *
 * This service provides means of indexing and querying generic information about models.
 *
 * @author Raza Ali, raza.ali@ebi.ac.uk
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 * @date   20160710
 */
class SearchService implements InitializingBean {
    /**
     * The class logger.
     */
    static final Log log = LogFactory.getLog(SearchService.class)
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
    /*
     * Dependency injection of grailsApplication
     */
    def redisService

    ModelSearchStrategy strategy

    SearchService() {
        loadSearchStrategy()
    }

    private void loadSearchStrategy() {
        String strategySetting = grails.util.Holders.grailsApplication.config.jummp.search.strategy
        if (!strategySetting) {
            log.error "Cannot load the setting model search strategy."
            strategySetting = "solr"
            log.error "... using the default value: ${strategySetting}"
        }
        strategy = strategySetting.equalsIgnoreCase("omicsdi") ? new OmicsdiBasedSearch() : new SolrBasedSearch()
        //setSearchStrategy("omicsdi") // For testing immediately without changing .jummp.properties
    }

    private void setSearchStrategy(String strategy) {
        this.strategy = strategy.equalsIgnoreCase("omicsdi") ? new OmicsdiBasedSearch() : new SolrBasedSearch()
    }

    /**
     * Clears the index. Handle with care.
     */
    @Secured(['ROLE_ADMIN'])
    @PostLogging(LoggingEventType.DELETION)
    @Profiled(tag="searchService.clearIndex")
    void clearIndex(RevisionTransportCommand revision = null) {
        if (revision) {
            log.info("Clearing the indexes of the ${revision.identifier()}.")
            strategy.clearIndex(revision)
        } else {
            log.info("Clearing all indexes from the database.")
            strategy.clearIndex()
            clearAnnotationStatementsFromDatabase()
        }
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
        clearIndex(revision)
        strategy.updateIndex(revision)
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
        strategy.clearIndex()
        List<RevisionTransportCommand> revisions = Revision.list(fetch: [model: "eager"]).collect { r ->
            new RevisionAdapter(revision: r).toCommandObject()
        }
        if (IS_DEBUG_ENABLED) {
            log.debug "Indexing ${revisions.size()} revisions."
        }
        Authentication auth = springSecurityService.authentication
        AtomicReference<Authentication> authRef = new AtomicReference<>(auth)
        Promise p = Revision.async.task {
            SecurityContextHolder.context.authentication = authRef.get()
            final int revisionCount = revisions.size()
            final AtomicInteger index = new AtomicInteger()
            revisions.each { revision ->
                try {
                    updateIndex(revision)
                }
                catch(Exception e) {
                    log.error("Exception thrown while indexing ${revision.properties} ${e.getMessage()}", e)
                } finally {
                    log.info "Revision ${revision.id} has been indexed. Iteration ${index.incrementAndGet()} of $revisionCount."
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
     * Returns search results for query restricted Models the user has access to.
     *
     * Executes the @p query, restricting results to Models the current user has access to.
     * @param query free text search on models
     * @return Collection of ModelTransportCommand of relevant models available to the user.
     **/
    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="searchService.searchModels")
    SearchResponse searchModels(String query, String domain, SortOrder sortOrder,
                                       Map<String, Integer> paginationCriteria) {
        return strategy.searchModels(query, domain, sortOrder, paginationCriteria)
    }

    Map extractSearchModels(SearchResponse response) {
        Integer totalCount = (Integer) response.totalCount
        ArrayList<ModelTransportCommand> results = response.results
        List<ModelTransportCommand> models = []
        List<Facet> facets = []
        if (results?.size() > 0) {
            results.each {
                models.add(it)
            }
        }
        LinkedHashMap<String, OrderedFacet> respondedFacets = response.facets
        if (respondedFacets?.size() > 0) {
            respondedFacets.each {
                facets.add(it.value.facet)
            }
        }
        log.info("Found: ${results?.size() ?: 0} records, ${respondedFacets?.size() ?: 0} facets.")
        JsonBuilder builder = new JsonBuilder(facets)
        String facetStats = builder.toString()

        doUpdateCachedSearchAll(totalCount, models, facets, facetStats)

        [totalCount: totalCount, models: models, facets: facets, facetStats: facetStats]
    }

    /*
     * Removes revision annotations from the database.
     *
     * This is necessary to ensure that we keep in sync Solr & OmicsDI entries with the database
     * at the start of the reindexing process.
     */
    @Profiled(tag = "searchService.clearAnnotationStatementsFromDatabase")
    void clearAnnotationStatementsFromDatabase() {
        log.debug("Begin cleaning annotation statements from database")
        Revision.executeUpdate("delete ElementAnnotation")
        Revision.executeUpdate("delete Statement")
        log.debug("Finished cleaning annotation statements from database")
    }

    Map checkIndexedData() {
        strategy.checkIndexedData()
    }

    String[] getSearchFields() {
        strategy.getSortFields()
    }

    Map retrieveCachedSearchAllResult() {
        if (!redisService.exists("search-result:all")) {
            return [:]
        }
        Map result = redisService.doRedisHGetAll("search-result:all")
        Integer matches = result.get("totalCount") as Integer
        String facetStats = result.get("facetStats")
        List<ModelTransportCommand> models = convert2MTC(result.get("models"))
        List<Facet> facets = convert2Facet(result.get("facets"))

        [matches: matches, models: models, facets: facets, facetStats: facetStats]
    }

    private void doUpdateCachedSearchAll(Integer totalCount, List<ModelTransportCommand> models,
                                         List<Facet> facets, String facetStats) {
        Map<String, String> data = ["totalCount": Integer.toString(totalCount), facetStats: facetStats]

        StringBuilder str = new StringBuilder()
        for (ModelTC model : models) {
            str.append strMTC(model)
        }
        data.put("models", str.toString())

        str = new StringBuilder()
        for (Facet facet : facets) {
            str.append(strFacet(facet))
        }
        data.put("facets", str.toString())

        redisService.doRedisHSet("search-result:all", data)
    }

    private static String strFacet(Facet facet) {
        // TODO: correct me
        facet.id + "|" + facet.label
    }

    private static String strMTC(ModelTC model) {
        // TODO: correct me
        model.id + "|" + model.name
    }

    private static Facet toFacet(final String input) {
        // TODO: implement me
        null
    }

    private static ModelTC toMTC(final String input) {
        // TODO: implement me
        null
    }


    private static List<ModelTC> convert2MTC(Object o) {
        // TODO: implement me
        null
    }

    private static List<Facet> convert2Facet(Object o) {
        null
    }

    List<Facet> buildBasicFacets(List<ModelTransportCommand> models) {
        List<Facet> facets = []
        def formats = models.collect(
            new HashSet(), {
            [it?.format?.identifier, it?.format?.name, 0]
        })
        Map<String, String> submitterMap = new HashMap<>()
        for (ModelTransportCommand m : models) {
            submitterMap.putAll(m.creatorUsernames)
        }
        def submitters = submitterMap.values().collect(
            new HashSet(), {
            [it, 0]
        })
        def types = new HashSet([["Private", 0], ["Shared", 0], ["Public", 0]])
        models.each {
            String format = it?.format?.identifier
            formats.each {
                if (it[0] == format) {
                    it[2] += 1
                }
            }

            String submitter = it.submitter
            submitters.each {
                if (it[0] == submitter) {
                    it[1] += 1
                }
            }

            String currentLoggedInUsername = springSecurityService.currentUser.username
            String submitterUsername = it.submitterUsername
            if (currentLoggedInUsername.equalsIgnoreCase(submitterUsername)) {
                types[0][1] += 1
            }
            boolean shared = !currentLoggedInUsername.equalsIgnoreCase(submitterUsername) && it.state == ModelState.UNPUBLISHED
            if (shared) {
                types[1][1] += 1
            }
            if (it.state == ModelState.PUBLISHED) {
                types[2][1] += 1
            }
        }
        // Format
        def facet = new Facet()
        List<FacetValue> fvs = []
        facet.id = "format"
        facet.label = "Format"
        facet.facetValues = []
        formats.each {
            FacetValue fv = new FacetValue(label: it[1], value: it[0], count: it[2])
            fvs << fv
        }
        facet.facetValues = fvs
        facets << facet
        // Submitter
        facet = new Facet()
        fvs = []
        facet.id = "submitter"
        facet.label = "Collaborator"
        facet.facetValues = []
        submitters.each {
            FacetValue fv = new FacetValue(label: it[0], value: it[0], count: it[1])
            fvs << fv
        }
        facet.facetValues = fvs
        facets << facet
        // Model Types: Private, Shared, Public
        facet = new Facet()
        fvs = []
        facet.id = "type"
        facet.label = "Type"
        facet.facetValues = []
        types.each {
            FacetValue fv = new FacetValue(label: it[0], value: it[0], count: it[1])
            fvs << fv
        }
        facet.facetValues = fvs
        facets << facet

        facets
    }

    @Override
    void afterPropertiesSet() throws Exception {
        log.info("Finished the bean initialisation")
    }
}

