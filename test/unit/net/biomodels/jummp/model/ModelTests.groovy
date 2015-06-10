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
* Grails (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Grails used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.model

import grails.test.*
import grails.test.mixin.TestFor
import net.biomodels.jummp.plugins.security.Person
import net.biomodels.jummp.plugins.security.User

@TestFor(Model)
class ModelTests {

    void testConstraints() {
        mockForConstraintsTests(Model, [new Model(name: "test", vcsIdentifier: "vcs")])
        // test for nullable
        Model model = new Model()
        assertFalse(model.validate())
        assertEquals("nullable", model.errors["vcsIdentifier"])
        assertEquals("nullable", model.errors["revisions"])
        model = new Model()
        model.vcsIdentifier = ""
        assertFalse(model.validate())
        assertEquals("blank", model.errors["vcsIdentifier"])
        assertEquals("nullable", model.errors["revisions"])
        // test for uniqueness
        model = new Model()
        model.vcsIdentifier = "vcs"
        assertFalse(model.validate())
        assertEquals("unique", model.errors["vcsIdentifier"])
        assertEquals("nullable", model.errors["revisions"])
        // test for the Revisions being empty
        model = new Model()
        model.revisions = []
        assertFalse(model.validate())
        assertEquals("nullable", model.errors["vcsIdentifier"])
        assertEquals("validator", model.errors["revisions"])
        model = new Model()
        Person testUser = new Person(userRealName: 'Test User')
        User owner = new User(username: "testUser", password: "secret", person: testUser,
                email: "test@user.org", enabled: true, accountExpired: false,
                accountLocked: false, passwordExpired: false)
        Revision revision = new Revision(model: model, vcsId: "2", revisionNumber: 2,
                owner: owner, minorRevision: true, uploadDate: new Date(), name:'test',
                description:'pointless', comment: 'fictional',
                format: new ModelFormat(identifier: "UNKNOWN", name: "unknown"))
        mockDomain(Revision, [revision])
        model.revisions = [revision] as Set
        model.vcsIdentifier = "1234"
        assertFalse model.validate()
        assertEquals 1, model.errors.errorCount
        model.submissionId = ""
        assertFalse model.validate()
        assertEquals 1, model.errors.errorCount
        // try a valid model
        model.submissionId = "MODEL1234"
        assertTrue(model.validate())
        assertFalse(model.hasErrors())
    }
}
