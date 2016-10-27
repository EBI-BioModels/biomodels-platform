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

import grails.plugin.springsecurity.annotation.Secured
import net.biomodels.jummp.core.events.LoggingEventType
import net.biomodels.jummp.core.events.PostLogging
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.search.OmicsdiBasedSearch
import net.biomodels.jummp.search.SearchResponse
import net.biomodels.jummp.search.SolrBasedSearch
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.perf4j.aop.Profiled

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
class SearchService {
    /**
     * The class logger.
     */
    static final Log log = LogFactory.getLog(SearchService)
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
    def grailsApplication

    ModelSearchStrategy strategy

    SearchService() {
        loadSearchStrategy()
    }

    private void loadSearchStrategy() {
        String strategySetting = grails.util.Holders.grailsApplication.config.jummp.search.strategy
        if (strategySetting)
            log.info "Loaded search strategy: ${strategySetting}"
        else {
            log.error "Cannot load the value of search strategy property."
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
    void clearIndex() {
        strategy.clearIndex()
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
        strategy.regenerateIndices()
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
    SearchResponse searchModels(String query, Map<String, Integer> paginationCriteria) {
        return strategy.searchModels(query, paginationCriteria)
    }

    /*
     * Removes revision annotations from the database.
     *
     * This is necessary to ensure that we keep in sync Solr with the database
     * at the start of the reindexing process.
     */
    @Profiled(tag = "searchService.clearAnnotationStatementsFromDatabase")
    void clearAnnotationStatementsFromDatabase() {
        strategy.clearAnnotationStatementsFromDatabase()
    }
}

