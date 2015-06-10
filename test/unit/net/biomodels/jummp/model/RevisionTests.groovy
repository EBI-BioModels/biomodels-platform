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

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import net.biomodels.jummp.plugins.security.Person
import net.biomodels.jummp.plugins.security.User

@TestFor(Revision)
@Mock(Model)
class RevisionTests {
    @SuppressWarnings('UnusedVariable')
    void testConstraints() {
        Model model = new Model(vcsIdentifier: "test", name: "test")
        Person testUser = new Person(userRealName: 'Test User')
        User owner = new User(username: "testUser", password: "secret", person: testUser,
                email: "test@user.org", enabled: true, accountExpired: false,
                accountLocked: false, passwordExpired: false)
        mockForConstraintsTests(Revision, [new Revision(vcsId: "1", revisionNumber: 1,
                minorRevision: false, uploadDate: new Date(), comment: '', owner: owner,
                model: model)])
        // verify nullable
        Revision revision = new Revision()
        assertFalse(revision.validate())
        assertEquals("nullable", revision.errors["vcsId"])
        assertEquals("nullable", revision.errors["revisionNumber"])
        assertEquals("nullable", revision.errors["minorRevision"])
        assertEquals("nullable", revision.errors["uploadDate"])
        assertEquals("nullable", revision.errors["owner"])
        assertEquals("nullable", revision.errors["format"])
        assertNull(revision.errors["name"])
        assertNull(revision.errors["description"])
        assertNull(revision.errors["comment"])

        // a comment can be blank
        revision = new Revision(comment: '')
        assertFalse(revision.validate())
        assertNull(revision.errors["comment"])

        // verify vcsId uniqueness constraint
        revision = new Revision(vcsId: "1", model: model)
        assertFalse(revision.validate())
        assertEquals("unique", revision.errors["vcsId"])

        // verify revisionNumber uniqueness for a single model
        revision = new Revision(vcsId: "2", model: model, revisionNumber: 1)
        assertFalse(revision.validate())
        assertEquals("unique", revision.errors["revisionNumber"])

        // verify the comment constraints
        String comment = ""
        for (int i=0; i<=1000; i++) {
            comment += "1"
        }
        revision = new Revision(comment: comment)
        assertFalse(revision.validate())
        assertEquals("maxSize", revision.errors["comment"])
        // a comment may be smaller than 1000
        comment = ""
        for (int i=0; i<1000; i++) {
            comment += "1"
        }
        revision = new Revision(comment: comment)
        assertFalse(revision.validate())
        assertNull(revision.errors["comment"])

        // verify description constraints
        // a description can be blank
        revision = new Revision(description: '')
        assertFalse(revision.validate())
        assertNull(revision.errors["description"])

        // verify name constraints
        // a name can be blank
        revision = new Revision(name: '')
        assertFalse(revision.validate())
        assertNull(revision.errors["name"])

        // verify that a correct Revision is valid
        revision = new Revision(model: model, vcsId: "2", revisionNumber: 2, owner: owner,
                minorRevision: true, uploadDate: new Date(), name:'test',
                description: 'pointless', comment: 'fictional',
                format: new ModelFormat(identifier: "UNKNOWN", name: "unknown"))
        assertTrue(revision.validate())
    }
}
