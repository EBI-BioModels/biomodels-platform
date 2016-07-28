/**
* Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
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





package net.biomodels.jummp.core.user

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority

/**
 * @short Concrete Implementation of the JummpAuthentication.
 *
 * This Authentication is a small wrapper for a normal Authentication extended by
 * the AuthenticationHash.
 *
 * To create an instance of this class use the static factory method @link fromAuthentication.
 * @author Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 */
final class JummpAuthenticationImpl extends AbstractAuthenticationToken implements JummpAuthentication {
    private static final long serialVersionUID = 1L
    def principal
    private String authenticationHash

//    private JummpAuthenticationImpl() {}
//    private JummpAuthenticationImpl(Map map) {}

    private JummpAuthenticationImpl(Collection<GrantedAuthority> authorities) {
        super(authorities)
    }

    Object getCredentials() {
        return null
    }

    Object getPrincipal() {
        return principal
    }

    def String getAuthenticationHash() {
        return authenticationHash
    }

    /**
     * Creates an instance of this class from the passed @p authentication and @p authenticationHash.
     * @param authentication The Authentication used as source
     * @param authenticationHash The hash value for this Authentication
     * @return an instance of JummpAuthentication
     */
    public static JummpAuthentication fromAuthentication(Authentication authentication, String authenticationHash) {
        JummpAuthenticationImpl auth = new JummpAuthenticationImpl(authentication.getAuthorities())
        auth.setAuthenticated(authentication.isAuthenticated())
        auth.setDetails(authentication.getDetails())
        auth.principal = authentication.getPrincipal()
        auth.authenticationHash = authenticationHash
        return auth
    }
}
