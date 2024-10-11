package net.biomodels.jummp.core

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SearchIndexDbJob {
    /**
     * The class logger.
     */
    static final Logger LOGGER = LoggerFactory.getLogger(SearchIndexDbJob.class)

    def searchService

    static triggers = {
        // Every day at 18:00
        cron name: "searchIndexDB", cronExpression: "0 5 23 * * ?"
    }

    /**
     * Triggers the indexing the whole database for search.
     */
    def execute() {
        LOGGER.info "Started indexing (e.g., exporting OmicsDI XML files)..."
        searchService.indexDB()
        LOGGER.info "... finished indexing (e.g., exporting...)."
    }
}
