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
* Apache Commons, Grails, JUnit (or a modified version of that library), containing parts
* covered by the terms of Common Public License, Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Apache Commons, Grails, JUnit used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.model

import grails.test.mixin.*
import org.apache.commons.io.FileUtils
import org.junit.*
import net.biomodels.jummp.model.Revision

@TestFor(RepositoryFile)
class RepositoryFileTests {
    @Before
    public void setUp() {
        new File("target/vcs/aaa/model1/").mkdirs()
     }

    @After
    public void tearDown() {
        FileUtils.deleteQuietly(new File("target/vcs/aaa"))
    }

    @Test
    void testConstraints() {
        def f = createFile("target/vcs/aaa/model1/m1.xml")
        def repositoryFile = new RepositoryFile(path: f.absolutePath, description: "Model1",
               mainFile: true, hidden: false, mimeType: "application/xml", revision: null)
        mockForConstraintsTests(RepositoryFile, [repositoryFile])

        //nulls are not accepted
        repositoryFile = new RepositoryFile()
        assertFalse(repositoryFile.validate())
        // mimeType(nullable: false, blank: false) was added to RepositoryFile (AnnotationStore)
        // after this test was written, adding a 3rd required-field error alongside path/revision.
        assertEquals(3, repositoryFile.errors.getErrorCount())
        assertEquals("nullable", repositoryFile.errors["path"])
        assertEquals("nullable", repositoryFile.errors["revision"])
        assertEquals("nullable", repositoryFile.errors["mimeType"])

        // path cannot be blank
        def rev = new Revision(vcsId: "1", revisionNumber: 1, minorRevision: false,
                uploadDate: new Date(), comment: "", owner: null, model: null)
        repositoryFile = new RepositoryFile(path: "", description: "nothing", hidden: false,
                mainFile: false, mimeType: "application/octet-stream", revision: rev)
        assertFalse(repositoryFile.validate())
        assertEquals("nullable", repositoryFile.errors["path"])
        assertEquals(1, repositoryFile.errors.getErrorCount())

        // Empty file descriptions are acceptable (nullable: true, no blank: false), but mimeType
        // has since gained blank: false ("the mime type should be known by now") - an empty one
        // is no longer valid, so a real value is supplied here instead.
        def newPath = createFile("target/vcs/aaa/model1/m1a.xml").absolutePath
        assertTrue(new File(newPath).exists())
        def repositoryFile2 = new RepositoryFile(path: newPath, description: "", mainFile: false,
                mimeType: "application/octet-stream", hidden: false, revision: new Revision())
        assertTrue(repositoryFile2.validate())

    }

    private File createFile(String path) {
        if (path == null) {
            path = "error.xml"
        }
        def f = new File(path)
        f.text = '''<?xml version="1.0"?>
    <foo>
        <bar>baz</bar>
    </foo>
'''
        return f.getCanonicalFile()
    }
}
