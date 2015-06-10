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

/**
* @author Raza Ali <raza.ali@ebi.ac.uk>
* @author Mihai Glonț <mihai.glont@ebi.ac.uk>
*/


package net.biomodels.jummp.model

import net.biomodels.jummp.plugins.security.Person

import grails.test.*

@TestFor(Publication)
class PublicationTests {
    void testConstraints() {
        mockForConstraintsTests(Publication)
        // test for nullable
        Publication publication = new Publication()
        assertFalse(publication.validate())
        assertEquals("nullable", publication.errors["journal"])
        assertEquals("nullable", publication.errors["title"])
        assertEquals("nullable", publication.errors["affiliation"])
        assertEquals("nullable", publication.errors["synopsis"])
        // test for blank
        publication = new Publication(journal: "", title: "", affiliation: "", synopsis: "")
        assertFalse(publication.validate())
        assertEquals("nullable", publication.errors["journal"])
        assertEquals("nullable", publication.errors["title"])
        assertEquals("nullable", publication.errors["affiliation"])
        assertEquals("nullable", publication.errors["synopsis"])
        // test for max size of synopsis
        String synopsis = ""
        for (int i=0; i<=5000; i++) {
            synopsis += "1"
        }
        publication.synopsis = synopsis
        assertFalse(publication.validate())
        assertEquals("maxSize", publication.errors["synopsis"])
        // test for allowed values
        publication = new Publication(journal: "journal", title: "title", affiliation: "Affiliation")
        synopsis = ""
        for (int i=1; i<1000; i++) {
            synopsis += "1"
        }
        publication.synopsis = synopsis
        assertTrue(publication.validate())
    }
}
