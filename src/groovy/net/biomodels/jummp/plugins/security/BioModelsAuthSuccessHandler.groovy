/**
 * Copyright (C) 2010-2023 EMBL-European Bioinformatics Institute (EMBL-EBI),
 * Deutsches Krebsforschungszentrum (DKFZ)
 *
 * This file is part of Jummp.
 *
 * Jummp is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along
 * with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
 *
 * Additional permission under GNU Affero GPL version 3 section 7
 *
 * If you modify Jummp, or any covered work, by linking or combining it with
 * Spring Framework, Spring Security (or a modified version of that library), containing parts
 * covered by the terms of Apache License v2.0, the licensors of this
 * Program grant you additional permission to convey the resulting work.
 * {Corresponding Source for a non-source form of such a combination shall
 * include the source code for the parts of Spring Framework, Spring Security used as well as
 * that of the covered work.}
 **/

package net.biomodels.jummp.plugins.security

import grails.plugin.springsecurity.web.authentication.AjaxAwareAuthenticationSuccessHandler as AAASH
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication

import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * This class customises the post process after the successful login. It changes the default behaviour a bit by
 * redirecting the user to the previous page which is the page of an unpublished model.
 * I relearned the implementation from this post
 * https://groggyman.com/2015/04/05/custom-authentication-success-handler-with-grails-and-spring-security/
 */
class BioModelsAuthSuccessHandler extends AAASH {
    private static final Logger LOGGER = LoggerFactory.getLogger(BioModelsAuthSuccessHandler.class)

    def loginAttemptCacheService
    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
        String preURL = request.getParameter("j_previousURL")
        boolean isUnpublishedModel = preURL?.indexOf("/biomodels/MODEL")  >= 0
        if (isUnpublishedModel) {
            String username = request.getParameter("username")
            LOGGER.debug("The user '${username}' has logged in to access this unpublished model $preURL.")
        }
        return preURL
    }

    @Override
    void onAuthenticationSuccess(final HttpServletRequest request, final HttpServletResponse response,
                                 final Authentication authentication) throws ServletException, IOException {
        try {
            String username = authentication.principal as String
            String warningMessage
            if (username) {
                warningMessage = loginAttemptCacheService.loginSuccess(username)
            } else {
                warningMessage = "Cannot recognise the username who has tried to log in."
            }
            LOGGER.error(warningMessage)
            handle(request, response, authentication)
            super.clearAuthenticationAttributes(request)
        } finally {
            // always remove the saved request
            requestCache.removeRequest(request, response)
        }
    }

    protected void handle(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
        throws IOException, ServletException {
        String targetUrl = determineTargetUrl(request, response)

        if (response.isCommitted()) {
            logger.debug("Response has already been committed. Unable to redirect to $targetUrl")
            return
        }

        redirectStrategy.sendRedirect(request, response, targetUrl)
    }
}
