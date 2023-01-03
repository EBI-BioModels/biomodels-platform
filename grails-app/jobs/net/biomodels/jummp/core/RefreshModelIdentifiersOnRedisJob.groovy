package net.biomodels.jummp.core

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RefreshModelIdentifiersOnRedisJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshModelIdentifiersOnRedisJob.class)

    def modelService

    static triggers = {
        // execute job every three minutes for development;
        // has to be set an appropriate repeat interval later for testing
        // simple name: 'simpleTrigger', startDelay: 10000, repeatInterval: 30000, repeatCount: 10

        simple name: "updateAllModelIdentifiersCachedOnRedis", startDelay: 1000*60*3, repeatInterval: 1000*60*3

        // the job is run at 02:00 A.M. daily
//        cron name: "updateAllModelIdentifiersCachedOnRedis", cronExpression: "0 0 2 * * ?"
    }

    void execute() {
        String msgLog = """\
QuartzJob: Started updating the cached model identifiers on Redis."""
        LOGGER.info(msgLog)
        println(msgLog)
        try {
            println "called modelService.extractAndCacheAllModelIdentifiersFromEBISearchServer()"
            modelService.extractAndCacheAllModelIdentifiersFromEBISearchServer()
        } catch (SocketTimeoutException ste) {
            msgLog = """\
There have been some connections timed out when trying to hit EBI Server and update the cached model identifiers. \
Please try to update Redis cache for the model manually."""
            LOGGER.debug(msgLog, ste)
            println(msgLog)
        }
        msgLog = "QuartzJob: Completed updating the cached model identifiers on Redis."
        LOGGER.info(msgLog)
        println(msgLog)
    }
}
