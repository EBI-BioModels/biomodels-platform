package net.biomodels.jummp.plugins.security

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.web.authentication.AjaxAwareAuthenticationFailureHandler as AAAFH
import grails.util.Holders
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.core.AuthenticationException

import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
/**
 * Registers all failed attempts to login. Main purpose to count attempts for particular account ant block user
 */
class BioModelsAuthFailureHandler extends AAAFH {
    private static final Logger LOGGER = LoggerFactory.getLogger(AAAFH.class)
    def loginAttemptCacheService //= Holders.applicationContext.getBean "loginAttemptCacheService"

    @Override
    void onAuthenticationFailure(final HttpServletRequest request, final HttpServletResponse response,
        final AuthenticationException exception) throws IOException, ServletException {
        String username = request.getParameter("username") ?: "teo"
        loginAttemptCacheService.failLogin(username)
        if (SpringSecurityUtils.isAjax(request)) {
            saveException(request, exception);
            getRedirectStrategy().sendRedirect(request, response, ajaxAuthenticationFailureUrl);
        } else {
            super.onAuthenticationFailure(request, response, exception);
        }
    }
}
