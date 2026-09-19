/**
* Copyright (C) 2010-2016 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
**/

package net.biomodels.jummp.plugins.security

import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpRequestResponseHolder
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.web.util.UrlPathHelper

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Keeps a session anonymous until its second authentication factor has been verified.
 *
 * <p>After the password check the session already holds the fully authenticated user, and
 * {@code session.enabled2FA} stays set until the one-time passcode is entered. The security layer used to
 * ignore that flag, so the half-authenticated session could still read whatever the user's ACLs allow (e.g. a
 * private model) from any other tab on pages that are public, while only the header pretended to be logged out.</p>
 *
 * <p>While the flag is set, every request is given an empty context (which the anonymous filter turns into an
 * anonymous user), except those that must know who is completing the check: {@code /auth/**}, the login
 * form and the logout. The stored context is left untouched, so it becomes effective again as soon as the flag
 * is cleared by a successful verification.</p>
 *
 * <p>For such anonymous requests the delegate is deliberately not asked to load the context. Its response
 * wrapper would otherwise persist the anonymous context when the response is committed, which removes the
 * user's login from the session.</p>
 */
class TwoFactorAwareSecurityContextRepository implements SecurityContextRepository {
    /** Session flag set by {@link BioModelsAuthSuccessHandler} while the OTP is still to be verified. */
    static final String OTP_PENDING_SESSION_KEY = "enabled2FA"
    static final String ANONYMOUS_VIEW_REQUEST_ATTRIBUTE = TwoFactorAwareSecurityContextRepository.name + ".ANONYMOUS_VIEW"
    /** Paths, within the application, that need the real identity while the OTP is pending. */
    static final List<String> TWO_FACTOR_PATHS = ["/auth"].asImmutable()

    SecurityContextRepository wrapped
    /** Extra paths that need the real identity, e.g. the login form action and the logout URL. */
    List<String> realIdentityPaths = []

    private final UrlPathHelper urlPathHelper = new UrlPathHelper()

    @Override
    SecurityContext loadContext(HttpRequestResponseHolder holder) {
        HttpServletRequest request = holder.request
        if (isOtpPending(request) && !needsRealIdentity(request)) {
            request.setAttribute(ANONYMOUS_VIEW_REQUEST_ATTRIBUTE, Boolean.TRUE)
            return SecurityContextHolder.createEmptyContext()
        }
        return wrapped.loadContext(holder)
    }

    @Override
    void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
        if (request.getAttribute(ANONYMOUS_VIEW_REQUEST_ATTRIBUTE)) {
            // the context of this request is not the session's; keep the stored one for after the verification
            return
        }
        wrapped.saveContext(context, request, response)
    }

    @Override
    boolean containsContext(HttpServletRequest request) {
        return wrapped.containsContext(request)
    }

    private static boolean isOtpPending(HttpServletRequest request) {
        return request.getSession(false)?.getAttribute(OTP_PENDING_SESSION_KEY) as boolean
    }

    private boolean needsRealIdentity(HttpServletRequest request) {
        String path = urlPathHelper.getPathWithinApplication(request)
        return (TWO_FACTOR_PATHS + realIdentityPaths).any { String base ->
            base && (path == base || path.startsWith(base + "/"))
        }
    }
}
