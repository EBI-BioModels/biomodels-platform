package net.biomodels.jummp

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.test.spock.IntegrationSpec
import net.biomodels.jummp.plugins.security.TwoFactorAwareSecurityContextRepository
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.SecurityContextPersistenceFilter
import spock.lang.Unroll

/**
 * The 2FA gate only works if the security filters use TwoFactorAwareSecurityContextRepository, i.e. if the bean in
 * resources.groovy replaces the plugin's securityContextRepository and wraps a session-backed repository.
 */
class TwoFactorSecurityWiringITSpec extends IntegrationSpec {
    def securityContextRepository
    def securityContextPersistenceFilter
    def springSecurityFilterChain

    void "the security context repository of the filter chain is the 2FA-aware one"() {
        expect:
        securityContextRepository instanceof TwoFactorAwareSecurityContextRepository
        securityContextPersistenceFilter.repo.is(securityContextRepository)
    }

    @Unroll
    void "no remember-me cookie is honoured on #uri"() {
        when:
        def filters = springSecurityFilterChain.getFilters(uri)

        then: "the chain is the session one, and a cookie issued at the password step cannot skip the OTP"
        filters.any { it instanceof SecurityContextPersistenceFilter }
        !filters.any { it instanceof RememberMeAuthenticationFilter }

        where:
        uri << ["/MODEL2504160001", "/user", "/search", "/j_spring_security_check", "/auth/verifyOTP"]
    }

    void "it wraps the session repository and lets the login and logout requests through"() {
        expect:
        securityContextRepository.wrapped instanceof HttpSessionSecurityContextRepository
        def conf = SpringSecurityUtils.securityConfig
        securityContextRepository.realIdentityPaths == [conf.apf.filterProcessesUrl, conf.logout.filterProcessesUrl]
    }
}
