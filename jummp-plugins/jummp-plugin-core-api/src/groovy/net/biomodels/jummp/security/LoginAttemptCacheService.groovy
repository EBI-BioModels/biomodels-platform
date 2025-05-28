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

class LoginAttemptCacheService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginAttemptCacheService.class)
    private LoadingCache attempts
    private int allowedNumberOfAttempts
    def grailsApplication

    @PostConstruct
    void init() {
        allowedNumberOfAttempts = grailsApplication.config.brutforce.loginAttempts.allowedNumberOfAttempts
        int time = grailsApplication.config.brutforce.loginAttempts.time

        LOGGER.info "account block configured for $time minutes"
        attempts = CacheBuilder.newBuilder()
                .expireAfterWrite(time, TimeUnit.MINUTES)
                .build({0} as CacheLoader)
    }

    /**
     * Triggers on each unsuccessful login attempt and increases number of attempts in local accumulator
     * @param login - username which is trying to login
     * @return
     */
    def failLogin(String login) {
        def numberOfAttempts = attempts.get(login)
        LOGGER.debug "fail login $login previous number for attempts $numberOfAttempts"
        numberOfAttempts++

        if (numberOfAttempts > allowedNumberOfAttempts) {
            blockUser(login)
            attempts.invalidate(login)
        } else {
            attempts.put(login, numberOfAttempts)
        }
    }

    /**
     * Triggers on each successful login attempt and resets number of attempts in local accumulator
     * @param login - username which is login
     */
    def loginSuccess(String login) {
        LOGGER.debug "successfully login for $login"
        attempts.invalidate(login)
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
        });

    }
}
