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

import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.web.context.HttpRequestResponseHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import spock.lang.Specification
import spock.lang.Unroll

/**
 * A session that has passed the password check but not the OTP must be anonymous outside the pages that complete
 * the verification, otherwise it can read private models from another browser tab.
 */
class TwoFactorAwareSecurityContextRepositorySpec extends Specification {
    private static final String KEY = HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY

    TwoFactorAwareSecurityContextRepository repository = new TwoFactorAwareSecurityContextRepository(
            wrapped: new HttpSessionSecurityContextRepository(),
            realIdentityPaths: ["/j_spring_security_check", "/j_spring_security_logout"])
    MockHttpSession session = new MockHttpSession()
    MockHttpServletResponse response = new MockHttpServletResponse()
    SecurityContext storedContext = new SecurityContextImpl(
            authentication: new UsernamePasswordAuthenticationToken("curator", "n/a",
                    AuthorityUtils.createAuthorityList("ROLE_USER")))

    def setup() {
        session.setAttribute(KEY, storedContext)
    }

    private MockHttpServletRequest requestFor(String uri, String contextPath = "") {
        def request = new MockHttpServletRequest("GET", contextPath + uri)
        request.contextPath = contextPath
        request.session = session
        return request
    }

    private SecurityContext load(MockHttpServletRequest request) {
        return repository.loadContext(new HttpRequestResponseHolder(request, response))
    }

    void "without a pending OTP the stored context is used"() {
        when:
        def context = load(requestFor("/MODEL2504160001"))

        then:
        context.authentication.name == "curator"
    }

    @Unroll
    void "a session whose OTP flag is #flag keeps its identity"() {
        given:
        session.setAttribute("enabled2FA", flag)

        expect:
        load(requestFor("/MODEL2504160001")).authentication.name == "curator"

        where:
        flag << [Boolean.FALSE, null]
    }

    @Unroll
    void "a pending OTP makes #uri anonymous"() {
        given:
        session.setAttribute("enabled2FA", true)

        expect:
        load(requestFor(uri)).authentication == null

        where:
        uri << ["/MODEL2504160001", "/MODEL2504160001.xml", "/search", "/user", "/", "/authors", "/login/auth"]
    }

    @Unroll
    void "a pending OTP still identifies the user on #uri"() {
        given:
        session.setAttribute("enabled2FA", true)

        expect:
        load(requestFor(uri)).authentication.name == "curator"

        where:
        uri << ["/auth/two-factor-authentication", "/auth/enroll-two-factor", "/auth/verifyOTP",
                "/auth/request-new-verification-code", "/j_spring_security_check", "/j_spring_security_logout"]
    }

    void "the application context path is not part of the path that is matched"() {
        given:
        session.setAttribute("enabled2FA", true)

        expect:
        load(requestFor("/auth/verifyOTP", "/biomodels")).authentication.name == "curator"
        load(requestFor("/MODEL2504160001", "/biomodels")).authentication == null
    }

    void "a path that only starts with the name of a real-identity one is anonymous"() {
        given:
        session.setAttribute("enabled2FA", true)

        expect:
        load(requestFor("/authors")).authentication == null
    }

    void "the anonymous request does not remove the stored login, even when its response is committed"() {
        given:
        session.setAttribute("enabled2FA", true)
        def request = requestFor("/MODEL2504160001")
        def holder = new HttpRequestResponseHolder(request, response)

        when: "the anonymous filter fills the empty context and the response is sent before the chain ends"
        def context = repository.loadContext(holder)
        context.authentication = new AnonymousAuthenticationToken("key", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"))
        holder.response.sendRedirect("/somewhere")
        repository.saveContext(context, request, holder.response)

        then:
        session.getAttribute(KEY).is(storedContext)
    }

    void "the stored login is effective again once the OTP flag is cleared"() {
        given:
        session.setAttribute("enabled2FA", true)
        load(requestFor("/MODEL2504160001"))

        when:
        session.removeAttribute("enabled2FA")

        then:
        load(requestFor("/MODEL2504160001")).authentication.name == "curator"
    }

    void "a login submitted during a pending OTP replaces the stored context"() {
        given:
        session.setAttribute("enabled2FA", true)
        def request = requestFor("/j_spring_security_check")
        def holder = new HttpRequestResponseHolder(request, response)
        def context = repository.loadContext(holder)
        def newLogin = new UsernamePasswordAuthenticationToken("someone-else", "n/a",
                AuthorityUtils.createAuthorityList("ROLE_USER"))

        when:
        context.authentication = newLogin
        repository.saveContext(context, request, holder.response)

        then:
        session.getAttribute(KEY).authentication.name == "someone-else"
    }
}
