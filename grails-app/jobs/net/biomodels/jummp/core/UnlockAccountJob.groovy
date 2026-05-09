package net.biomodels.jummp.core

import grails.util.Holders
import net.biomodels.jummp.plugins.security.User
import org.quartz.Job
import org.quartz.JobDataMap
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionCallbackWithoutResult
import org.springframework.transaction.support.TransactionTemplate

/**
 * This job is define to unlock an account automatically after one hour since the account is locked.
 */
class UnlockAccountJob implements Job {
    static final Logger LOGGER = LoggerFactory.getLogger(UnlockAccountJob.class)
    def grailsApplication = Holders.grailsApplication

    static triggers = {
        // nothing
    }

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap()
        long userId = dataMap.getLongFromString("userId")
        PlatformTransactionManager ptm = grailsApplication.mainContext.getBean(PlatformTransactionManager.class)
        TransactionTemplate tx = new TransactionTemplate(ptm)
        tx.execute(new TransactionCallbackWithoutResult() {
            void doInTransactionWithoutResult(TransactionStatus status) {
                def user = User.get(userId)
                if (user) {
                    user.accountLocked = false
                    LOGGER.info("Unlocking the user ${user?.username}...")
                    if (!user.save(flush: true)) {
                        LOGGER.error("Unlocking the user ${user?.username} failed.")
                    } else {
                        LOGGER.error("Unlocking the user ${user?.username} succeeded.")
                    }
                }
            }
        })
    }
}
