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
* JUnit (or a modified version of that library), containing parts
* covered by the terms of Common Public License, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of JUnit used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.core

import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin
import net.biomodels.jummp.plugins.security.Role
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.plugins.security.Person
import net.biomodels.jummp.plugins.security.UserRole
import org.junit.*
import static org.junit.Assert.*

@TestMixin(IntegrationTestMixin)
class CreateAdminTests extends JummpIntegrationTest {
    def userService
    @Before
    void setUp() {
    }

    @After
    void tearDown() {
    }

    @Test
    void testCreateAdmin() {
        def person = new Person(userRealName: "Administrator")
        User user = new User(username: "admin", password: "1234", person: person, email: "admin@test.com")
        person.save()
        authenticateAnonymous()
        user.enabled = false
        assertFalse(user.validate())
        user.enabled = true
        // Other required values are not set
        assertFalse(user.validate())
        user.accountExpired = false
        user.accountLocked = false
        user.passwordExpired = false
        assertTrue(user.validate())
        assertTrue(userService.persistAdminWithRoles(user))
        assertFalse(userService.createRolesForAdmin(user))
        //To be sure, the user cannot be created twice
        assertFalse(userService.persistAdminWithRoles(user))
    }

    @Test
    void testCreateRolesForAdmin() {
        def person = new Person(userRealName: "Administrator")
        User user = new User(username: "admin", password: "1234", person: person, email: "admin@test.com")
        authenticateAnonymous()
        user.enabled = true
        user.accountExpired = false
        user.accountLocked = false
        user.passwordExpired = false
        person.save()
        assertNotNull(user.save())
        boolean ok = userService.createRolesForAdmin(user)
        assertTrue(ok)
        Role adminRole = Role.findByAuthority("ROLE_ADMIN")
        UserRole adminUserRole = UserRole.findByUserAndRole(user, adminRole)
        assertNotNull(adminUserRole)
        Role userRole = Role.findByAuthority("ROLE_USER")
        UserRole userUserRole = UserRole.findByUserAndRole(user, userRole)
        assertNotNull(userUserRole)
    }
}
