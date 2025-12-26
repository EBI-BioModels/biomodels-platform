package net.biomodels.jummp.plugins.security

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.web.authentication.AjaxAwareAuthenticationFailureHandler as AAAFH
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException

import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
/**
 * Registers all failed attempts to login. Main purpose to count attempts for particular account ant block user
 */
class BioModelsAuthFailureHandler extends AAAFH {
    private static final Logger LOGGER = LoggerFactory.getLogger(AAAFH.class)
    def userService
    def loginAttemptCacheService

    @Override
    void onAuthenticationFailure(final HttpServletRequest request, final HttpServletResponse response,
        AuthenticationException exception) throws IOException, ServletException {
        String username = exception.authentication.principal as String
        String warningMessage
        if (username) {
            String redirectURL = userService.isAllowedMigrationAWS(username)
            if (redirectURL) {
                request.session.setMaxInactiveInterval(0)
                redirectStrategy.sendRedirect(request, response, redirectURL)
            }
            warningMessage = loginAttemptCacheService.failLogin(username)
        } else {
            warningMessage = "Cannot recognise the username who has tried to log in."
        }
        LOGGER.error(warningMessage)
        if (exception instanceof BadCredentialsException) {
             exception = new BadCredentialsException(warningMessage)
        }

        if (SpringSecurityUtils.isAjax(request)) {
            saveException(request, exception)
            getRedirectStrategy().sendRedirect(request, response, ajaxAuthenticationFailureUrl)
        } else {
            super.onAuthenticationFailure(request, response, exception)
        }
    }
}
