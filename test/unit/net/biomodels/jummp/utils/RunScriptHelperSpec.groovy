package net.biomodels.jummp.utils

import grails.test.mixin.Mock
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import net.biomodels.jummp.plugins.security.Role
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.plugins.security.UserRole
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import spock.lang.Specification

/**
 * Covers JBM-767: every run-script based tool extends RunScriptHelper, whose static initialiser builds an
 * authentication token for the admin account when the class is loaded. That used to fail with "Ambiguous
 * method overloading" when ADMIN_USERNAME was unset (fixed by defaulting it to "administrator") and still does
 * when the admin account does not exist in the database (fixed by checking for the user before dispatching).
 */
@TestMixin(GrailsUnitTestMixin)
@Mock([User, UserRole, Role])
class RunScriptHelperSpec extends Specification {
    // the static initialiser of RunScriptHelper runs once per JVM, on first use, and needs this account to
    // exist then - a failed class initialisation would break every later use of the class in the test run
    static final String ADMIN = System.getenv("ADMIN_USERNAME") ?: "administrator"

    void setup() {
        new User(username: ADMIN, password: "secret").save(validate: false, flush: true)
    }

    void "the admin account defaults to administrator when ADMIN_USERNAME is not set"() {
        expect:
        RunScriptHelper.ADMIN_USERNAME == ADMIN
        RunScriptHelper.adminAuth.principal == ADMIN
    }

    void "createTokenForUser builds a token carrying the roles of the user"() {
        given:
        User scriptUser = new User(username: "script-runner", password: "secret").save(validate: false)
        Role role = new Role(authority: "ROLE_CURATOR").save(validate: false)
        new UserRole(user: scriptUser, role: role).save(validate: false, flush: true)

        when:
        UsernamePasswordAuthenticationToken token = RunScriptHelper.createTokenForUser("script-runner")

        then:
        token.principal == "script-runner"
        token.authorities*.authority == ["ROLE_CURATOR"]
    }

    void "createTokenForUser says which user is missing instead of failing with an ambiguous overload"() {
        when:
        RunScriptHelper.createTokenForUser("no-such-user")

        then: "an AssertionError naming the user, not a GroovyRuntimeException about overloading"
        AssertionError e = thrown()
        e.message.contains("No user found with username 'no-such-user'")
    }

    void "createTokenForUser insists on a username"() {
        when:
        RunScriptHelper.createTokenForUser((String) username)

        then:
        AssertionError e = thrown()
        e.message.contains("Username required but not defined")

        where:
        username << [null, ""]
    }
}
