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
* JUnit, Spring Security (or a modified version of that library), containing parts
* covered by the terms of Common Public License, Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of JUnit, Spring Security used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.core

import static org.junit.Assert.*
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.plugins.security.Person
import grails.plugin.springsecurity.acl.AclSid
import net.biomodels.jummp.plugins.security.Role
import net.biomodels.jummp.plugins.security.UserRole
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import grails.plugin.springsecurity.SpringSecurityUtils
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.authority.GrantedAuthorityImpl
import grails.util.Holders

/**
 * Base class for Integration tests providing useful methods for creating users and authentication.
 * @author  Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 */
class JummpIntegrationTest {
    def authenticationManager=Holders.applicationContext.getBean("authenticationManager")
    def springSecurityService=Holders.applicationContext.getBean("springSecurityService")

    def shouldFail = { exception, code ->
        try {
            code.call()
            fail("Exception of type ${exception} was expected")
        } catch (Exception e) {
            if (!exception.isAssignableFrom(e.class)) {
                fail("Exception of type ${exception} expected but got ${e.class}")
            }
        }
    }

    /**
     * Creates three users and their roles:
     * @li testuser with password secret and role ROLE_USER
     * @li user with password verysecret and role ROLE_USER
     * @li admin with password 1234 and ROLE_ADMIN and ROLE_USER
     */
    public void createUserAndRoles() {
        User user, user2, admin, curator
        Person person
        if (!User.findByUsername("testuser")) {
        	person=new Person(userRealName: "Test")
        	user = new User(username: "testuser",
                    password: springSecurityService.encodePassword("secret"),
                    person: person,
                    email: "test@test.com",
                    enabled: true,
                    accountExpired: false,
                    accountLocked: false,
                    passwordExpired: false)
            assertNotNull(person.save(flush:true, failOnError:true))
            assertNotNull(user.save())
            assertNotNull(new AclSid(sid: user.username, principal: true).save(flush: true))
        } else {
            user = User.findByUsername("testuser")
        }
        if (!User.findByUsername("username")) {
            person=new Person(userRealName: "Test2")
        	user2 = new User(username: "username",
                    password: springSecurityService.encodePassword("verysecret"),
                    person: person,
                    email: "test2@test.com",
                    enabled: true,
                    accountExpired: false,
                    accountLocked: false,
                    passwordExpired: false)
            assertNotNull(person.save(flush:true, failOnError:true))
            assertNotNull(user2.save())
            assertNotNull(new AclSid(sid: user2.username, principal: true).save(flush: true))
        } else {
            user2 = User.findByUsername("username")
        }
        if (!User.findByUsername("admin")) {
            person=new Person(userRealName: "administrator")
        	admin = new User(username: "admin",
                    password: springSecurityService.encodePassword("1234"),
                    person: person,
                    email: "admin@test.com",
                    enabled: true,
                    accountExpired: false,
                    accountLocked: false,
                    passwordExpired: false)
            assertNotNull(person.save(flush:true, failOnError:true))
            assertNotNull(admin.save())
            assertNotNull(new AclSid(sid: admin.username, principal: true).save(flush: true))
        } else {
            admin = User.findByUsername("admin")
        }
        if (!User.findByUsername("curator")) {
            person = new Person(userRealName: "Curator")
        	assertNotNull(person.save(flush:true, failOnError:true))
            curator = new User(username: "curator",
                    password: springSecurityService.encodePassword("extremelysecret"),
                    person: person,
                    email: "curator@test.com",
                    enabled: true,
                    accountExpired: false,
                    accountLocked: false,
                    passwordExpired: false)
            assertNotNull(curator.save())
            assertNotNull(new AclSid(sid: curator.username, principal: true).save(flush: true))
        } else {
            curator = User.findByUsername("curator")
        }
        ensureRoleExists("ROLE_USER")
        Role userRole = Role.findByAuthority("ROLE_USER")
        createUserRoleIfNeeded(user, userRole)
        createUserRoleIfNeeded(user2, userRole)
        createUserRoleIfNeeded(admin, userRole)
        createUserRoleIfNeeded(curator, userRole)
        ensureRoleExists("ROLE_ADMIN")
        Role adminRole = Role.findByAuthority("ROLE_ADMIN")
        createUserRoleIfNeeded(admin, adminRole)
        ensureRoleExists("ROLE_CURATOR")
        Role curatorRole = Role.findByAuthority("ROLE_CURATOR")
        createUserRoleIfNeeded(curator, curatorRole)
    }

    private void createUserRoleIfNeeded(User user, Role role) {
    	if (!UserRole.findByUserAndRole(user, role)) {
    		UserRole.create(user, role, false)
    	}
    }

    private def ensureRoleExists(String _authority) {
        if (!Role.findByAuthority(_authority)) {
            new Role(authority: _authority).save()
        }
    }

    /**
     * Sets and authentication based on username and password.
     * @param username The name of the user
     * @param password The password of the user.
     * @return The Authentication object
     */
    protected def authenticate(String username, String password) {
        def authToken = new UsernamePasswordAuthenticationToken(username, password)
        def auth = authenticationManager.authenticate(authToken)
        SecurityContextHolder.getContext().setAuthentication(auth)
        return auth
    }

    /**
     * Modifies the ifAnyGranted method of SpringSecurityUtils to return @p admin value.
     * @param admin if @c true, all access to ifAnyGranted returns @c true, @c false otherwise
     */
    protected void modelAdminUser(boolean admin) {
        SpringSecurityUtils.metaClass.'static'.ifAnyGranted = { String parameter ->
            return admin
        }
    }

    /**
     * Sets the current authentication to testuser and does not model as admin user.
     * @return The testusers authentication
     */
    public def authenticateAsTestUser() {
        modelAdminUser(false)
        return authenticate("testuser", "secret")
    }

    /**
     * Sets the current authentication to user and does not models as admin user.
     * @return The users authentication
     */
    protected def authenticateAsUser() {
        modelAdminUser(false)
        return authenticate("username", "verysecret")
    }

    /**
     * Sets the current authentication to admin and models as admin user.
     * @return The admin authentication
     */
    protected def authenticateAsAdmin() {
        modelAdminUser(true)
        return authenticate("admin", "1234")
    }

     /**
     * Sets an anonymous authentication and does not model as admin user.
     * @return The anonymous authentication
     */
    protected def authenticateAnonymous() {
        modelAdminUser(false)
        def auth = new AnonymousAuthenticationToken("test", "Anonymous", [ new GrantedAuthorityImpl("ROLE_ANONYMOUS")])
        SecurityContextHolder.getContext().setAuthentication(auth)
        return auth
    }

    /**
     * Sets the current authentication to curator and does not models as admin user.
     * @return The curator authentication
     */
    protected def authenticateAsCurator() {
        modelAdminUser(false)
        return authenticate("curator", "extremelysecret")
    }
}
