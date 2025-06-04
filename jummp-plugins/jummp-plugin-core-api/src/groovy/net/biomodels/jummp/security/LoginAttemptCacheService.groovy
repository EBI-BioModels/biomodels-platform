package net.biomodels.jummp.security

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import net.biomodels.jummp.plugins.security.User
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionCallbackWithoutResult
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
        def numberOfAttempts = attempts.get(login) as int
        //def numberOfAttempts = redisService.doRedisHGet("Login-Attempts", login)
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
        String warningMessage = """<b>Invalid login credentials.</b><br/><b>Attempts remaining:</b> 
${remainingAttempts}<br/>\
${s1}\
<b>Warning</b>: After $allowedNumberOfAttempts consecutive failed login attempts, you account will be temporarily \
locked."""

        if (numberOfAttempts > allowedNumberOfAttempts) {
            blockUser(login)
            attempts.invalidate(login)
            //redisService.doRedisHDel("Login-Attempts", login)
            // Notes: the message below is the same as the i18n: springSecurity.errors.login.locked
            warningMessage = "Your account has been locked. Please contact an administrator or try again later."

        } else {
            attempts.put(login, numberOfAttempts)
            //redisService.doRedisHSet("Login-Attempts", login, numberOfAttempts.toString())
        }
        return warningMessage
    }

    /**
     * Triggers on each successful login attempt and resets number of attempts in local accumulator
     * @param login - username which is login
     */
    def loginSuccess(String login) {
        LOGGER.debug "successfully login for $login"
        attempts.invalidate(login)
        //redisService.doRedisHDel("Login-Attempts", login)
        LOGGER.info("Login failures for $login was reset.")
    }

    /**
     * Disable user account so it would not able to login
     * @param login - username that has to be disabled
     */
    private void blockUser(String login) {
        LOGGER.debug "blocking user: $login"
        PlatformTransactionManager ptm = grailsApplication.mainContext.getBean(PlatformTransactionManager.class)
        TransactionTemplate tx = new TransactionTemplate(ptm)
        tx.execute(new TransactionCallbackWithoutResult() {
            void doInTransactionWithoutResult(TransactionStatus status) {
                def user = User.findByUsername(login)
                if (user) {
                    user.accountLocked = true
                    user.save(flush: true)
                }
            }
        })

    }
}
