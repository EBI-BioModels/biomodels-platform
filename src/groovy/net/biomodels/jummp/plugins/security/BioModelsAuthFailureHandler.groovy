package net.biomodels.jummp.plugins.security

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.web.authentication.AjaxAwareAuthenticationFailureHandler as AAAFH
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.AuthenticationException

import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
/**
 * Registers all failed attempts to login. Main purpose to count attempts for particular account ant block user
 */
class BioModelsAuthFailureHandler extends AAAFH {
    private static final Logger LOGGER = LoggerFactory.getLogger(AAAFH.class)

    /** Session key holding the j_previousURL of a failed login, until LoginController.authfail() hands it back. */
    static final String FAILED_LOGIN_PREVIOUS_URL = "failedLoginPreviousURL"
    def userService
    def loginAttemptCacheService

    @Override
    void onAuthenticationFailure(final HttpServletRequest request, final HttpServletResponse response,
        AuthenticationException exception) throws IOException, ServletException {
        String username = exception.authentication.principal as String
        String warningMessage
        if (exception instanceof LockedException) {
            warningMessage = exception.message
        } else if (username) {
            String redirectURL = userService.isAllowedMigrationAWS(username)
            if (redirectURL) {
                request.session.setMaxInactiveInterval(0)
                redirectStrategy.sendRedirect(request, response, redirectURL)
                return
            }
            warningMessage = loginAttemptCacheService.failLogin(username)
        } else {
            warningMessage = "Cannot recognise the username who has tried to log in."
        }
        LOGGER.error(warningMessage)
        if (exception instanceof BadCredentialsException) {
             exception = new BadCredentialsException(warningMessage)
        }

        rememberPreviousUrl(request)
        if (SpringSecurityUtils.isAjax(request)) {
            saveException(request, exception)
            getRedirectStrategy().sendRedirect(request, response, ajaxAuthenticationFailureUrl)
        } else {
            super.onAuthenticationFailure(request, response, exception)
        }
    }

    /**
     * Keeps the page the user wanted across the redirects back to the login form. The failure url is fixed, so
     * j_previousURL would be lost, and the Referer header of the redirected login page is the login page itself.
     * The value is not trusted here: {@link BioModelsAuthSuccessHandler#validatedPreviousUrl} checks it after
     * the next successful login.
     */
    void rememberPreviousUrl(HttpServletRequest request) {
        String previousURL = request.getParameter("j_previousURL")?.trim()
        if (previousURL) {
            request.getSession().setAttribute(FAILED_LOGIN_PREVIOUS_URL, previousURL)
        }
    }
}
