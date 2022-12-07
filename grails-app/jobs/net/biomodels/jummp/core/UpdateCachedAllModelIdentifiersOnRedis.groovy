package net.biomodels.jummp.core

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class UpdateCachedAllModelIdentifiersOnRedis {
    private static final Logger LOGGER = LoggerFactory.getLogger(this.getClass())

    def modelService

    static triggers = {
        // execute job once in 10 seconds for development;
        // has to be set an appropriate repeat interval later for testing
//        simple name: "updateParametersCachedOnRedis", startDelay: 10000, repeatInterval: 7_200_000L

        // the job is run at 02:00 A.M. daily
        cron name: "updateAllModelIdentifiersCachedOnRedis", cronExpression: "0 0 2 * * ?"
    }

    def execute() {
        String msgLog = """\
QuartzJob: Started updating the cached parameters on Redis."""
        LOGGER.info(msgLog)
        println(msgLog)
        try {
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
