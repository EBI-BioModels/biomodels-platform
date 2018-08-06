package net.biomodels.jummp.core

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * @short Job for optimising the search index.
 *
 * The job is run at 02:00 AM every day.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
class SearchIndexOptimiserJob {
    /**
     * The class logger.
     */
    static final Log log = LogFactory.getLog(SearchService)
    /**
     * Flag indicating the logger's verbosity threshold.
     */
    static final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()
    /**
     * Dependency injection of SolrServerHolder.
     */
    def solrServerHolder

    static triggers = {
        cron name: "optimiseSearchIndexTrigger", cronExpression: "0 0 2 * * ?"
    }

    /**
     * Triggers the optimisation of the search index.
     */
    def execute() {
        if (IS_DEBUG_ENABLED) {
            log.debug "Started optimising the search index..."
        }
        solrServerHolder.solrClient.optimize()
        if (IS_DEBUG_ENABLED) {
            log.debug "... finished optimising the search index."
        }
    }
}
