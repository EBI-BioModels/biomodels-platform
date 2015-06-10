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

import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin
import org.apache.commons.io.FileUtils
import org.junit.*
import static org.junit.Assert.*

@TestMixin(IntegrationTestMixin)
class MiriamServiceTests {
    def grailsApplication
    def miriamService
    File wd
    File exportFile
    final String exportName = "export.xml"

    @Override
    @Before
    void setUp() {
        wd  = new File("target/miriam/")
        wd.mkdirs()
        assertTrue wd.exists()

        exportFile = new File(wd, exportName)
        assertNotNull exportFile

        String exportParentPath = wd.absolutePath
        grailsApplication.config.jummp.vcs.workingDirectory = exportParentPath
        miriamService.registryExport = exportFile
    }

    @Override
    @After
    void tearDown() {
        FileUtils.deleteDirectory(wd)
    }

    @Test
    void testUpdateMiriamResourcesSecurity() {
        String url = "http://www.ebi.ac.uk/miriam/main/export/xml/"
        assertFalse exportFile.exists()
        miriamService.updateMiriamResources(url)
        assertTrue exportFile.exists()
        assertTrue(exportFile.size() > 0)
    }
}
