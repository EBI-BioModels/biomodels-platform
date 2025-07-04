package net.biomodels.jummp.security

import net.biomodels.jummp.core.UnlockAccountJob
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import net.biomodels.jummp.plugins.security.User
import org.quartz.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionCallbackWithoutResult
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate

import javax.annotation.PostConstruct
import java.util.concurrent.TimeUnit

/**
 * <p><b>Aim</b>: Track and lock users if they exceed failure attempts.</p
 *
 * <p>Author: <a href="mailto:nvntung@gmail.com">Tung Nguyen</a></p>
 */
class LoginAttemptCacheService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginAttemptCacheService.class)
    private LoadingCache attempts
    private int allowedNumberOfAttempts
    def grailsApplication
    def redisService

    @PostConstruct
    void init() {
        allowedNumberOfAttempts = grailsApplication.config.brutforce.loginAttempts.allowedNumberOfAttempts
        int time = grailsApplication.config.brutforce.loginAttempts.time

        LOGGER.info "account block configured for $time minutes"
        attempts = CacheBuilder.newBuilder()
                .expireAfterWrite(time, TimeUnit.MINUTES)
                .build({0} as CacheLoader)
        // initiate the map of failed login attempts
        redisService.doRedisHSet("Login-Attempts", "unknown_username", "1000")
    }

    /**
     * Triggers on each unsuccessful login attempt and increases number of attempts in local accumulator
     * @param login - username which is trying to login
     * @return
     */
    String failLogin(String login) {
        if (!userExists(login)) {
            LOGGER.debug("Failed login attempt for non-existing username: $login")
            return "<b>Invalid login credentials.</b>"
        }

        def numberOfAttempts = attempts.get(login) as int
        if (!numberOfAttempts) {
            numberOfAttempts = 1
        } else {
            numberOfAttempts = Integer.valueOf(numberOfAttempts as String)
            numberOfAttempts++
        }
        LOGGER.debug "Failed to log in $login previous number for attempts $numberOfAttempts"
        def remainingAttempts = allowedNumberOfAttempts - numberOfAttempts
        String s1 = ""
        if (remainingAttempts > 0) {
            s1 = """You can try again. If no further attempts are made within the next hour, your attempt count \
will reset to $allowedNumberOfAttempts.<br/>"""
        }
        String warningMessage = """<b>Invalid login credentials.</b><br/><b>Attempts remaining:</b> \
${remainingAttempts}<br/>\
${s1}\
<b>Warning</b>: After $allowedNumberOfAttempts consecutive failed login attempts, your account will be temporarily \
locked."""

        if (numberOfAttempts > allowedNumberOfAttempts) {
            lockUser(login)
            attempts.invalidate(login)
            warningMessage = """\
Your account has been locked. Please try to log in again after an hour or contact us for further assistance."""
        } else {
            attempts.put(login, numberOfAttempts)
        }
        return warningMessage
    }

    private boolean userExists(String login) {
        PlatformTransactionManager ptm = grailsApplication.mainContext.getBean(PlatformTransactionManager.class)
        TransactionTemplate tx = new TransactionTemplate(ptm)
        tx.readOnly = true
        return tx.execute({ TransactionStatus status ->
            User.findByUsername(login) != null
        } as TransactionCallback)
    }

    /**
     * Triggers on each successful login attempt and resets number of attempts in local accumulator
     * @param login - username which is login
     */
    def loginSuccess(String login) {
        LOGGER.debug "User [$login] logged in successfully."
        attempts.invalidate(login)
        //redisService.doRedisHDel("Login-Attempts", login)
        LOGGER.info("All login failures for $login were reset if happened earlier.")
    }

    /**
     * Lock the user account so it would not able to login
     * @param login - username that has to be disabled
     */
    private void lockUser(String login) {
        LOGGER.debug "blocking user: $login"
        PlatformTransactionManager ptm = grailsApplication.mainContext.getBean(PlatformTransactionManager.class)
        TransactionTemplate tx = new TransactionTemplate(ptm)
        tx.execute(new TransactionCallbackWithoutResult() {
            void doInTransactionWithoutResult(TransactionStatus status) {
                def user = User.findByUsername(login)
                if (user) {
                    user.accountLocked = true
                    if (user.save(flush: true)) {
                        unlockUser(user.id)
                    }
                }
            }
        })

    }

    private void unlockUser(long userId) {
        def quartzScheduler = grailsApplication.mainContext.getBean('quartzScheduler')
        // JobDetailImpl is deprecated, so it is replaced with JobBuilder.newJob()
        // JobDetail jobDetail = new JobDetailImpl('UnlockAccountJob', 'group1', UnlockAccountJob)
        JobDataMap dataMap = new JobDataMap()
        dataMap.putAsString("userId", userId)
        JobDetail jobDetail = JobBuilder.newJob(UnlockAccountJob.class)
                .withIdentity("unlockAccount")
                .usingJobData(dataMap)
                .build()
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity('trigger1', 'group1')
                //.startNow()
                .startAt(DateBuilder.futureDate(1, DateBuilder.IntervalUnit.HOUR))
                //.startAt(DateBuilder.futureDate(3, DateBuilder.IntervalUnit.MINUTE))
                /*.withSchedule(SimpleScheduleBuilder.simpleSchedule()
                    .withRepeatCount(0)
                    .withIntervalInMinutes(60))*/
                //.withSchedule(SimpleScheduleBuilder.repeatSecondlyForever(10))
                .build()
        quartzScheduler.scheduleJob(jobDetail, trigger)
        LOGGER.info("The user ${userId} will be unlocked after 60 minutes")
    }
}
