package net.biomodels.jummp.plugins.security

import org.springframework.security.web.authentication.WebAuthenticationDetails

import javax.servlet.http.HttpServletRequest

class TwoFactorAuthenticationDetails extends WebAuthenticationDetails {

    /**
     * Records the remote address and will also set the session Id if a session
     * already exists (it won't create one).
     *
     * @param request that the authentication request was received from
     */
    TwoFactorAuthenticationDetails(HttpServletRequest request) {
        super(request)
    }
}
