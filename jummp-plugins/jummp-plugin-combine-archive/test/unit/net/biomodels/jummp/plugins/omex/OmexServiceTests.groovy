/**
* Copyright (C) 2010-2021 EMBL-European Bioinformatics Institute (EMBL-EBI),
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





package net.biomodels.jummp.plugins.omex

import grails.test.mixin.*
import net.biomodels.jummp.core.model.ModelFormatTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand
import org.apache.commons.io.FileUtils
import org.junit.*

@TestFor(OmexService)
class OmexServiceTests {

    @Test
    void testValidation() {
        def omexService = new OmexService()
        assertFalse(omexService.validate(null, null))
        assertFalse(omexService.validate([], []))
        assertFalse(omexService.validate([new File("inexistent")], []))
        def randomFile = new File("target/misc.txt")
        FileUtils.touch(randomFile)
        randomFile.setText("Hello")
        assertTrue randomFile.exists()
        assertFalse omexService.validate([randomFile], [])
        def omexFile = new File("test/files/sample archive.omex")
        assertTrue omexService.validate([omexFile], [])
    }

    @Test
    void testExtractName() {
        def omexService = new OmexService()
        List<File> modelFiles = null
        assertEquals("", omexService.extractName(modelFiles))
        modelFiles = [new File("test/files/sample archive.omex")]
        assertEquals("", omexService.extractName(modelFiles))
    }

    @Test
    void testExtractDescription() {
        def omexService = new OmexService()
        List<File> modelFiles = null
        assertEquals("", omexService.extractDescription(modelFiles))
        modelFiles = [new File("test/files/sample archive.omex")]
        assertEquals("", omexService.extractDescription(modelFiles))
    }

    @Test
    void testExtractAnno() {
        def omexService = new OmexService()
        assertEquals([], omexService.getAllAnnotationURNs(null))
        def omexFormat = new ModelFormatTransportCommand(identifier: "OMEX",
                name: "Open Modelling Exchange Format")
        def file = new RepositoryFileTransportCommand(path: "test/files/sample archive.omex",
                mainFile: true, hidden: false, userSubmitted: true)
        def revision = new RevisionTransportCommand(format: omexFormat, files: [file])

        assertEquals([], omexService.getAllAnnotationURNs(revision))
    }

    @Test
    void testGetPublicationAnno() {
        def omexService = new OmexService()
        assertEquals([], omexService.getPubMedAnnotation(null))
        def omexFormat = new ModelFormatTransportCommand(identifier: "OMEX",
                name: "Open Modelling Exchange Format")
        def file = new RepositoryFileTransportCommand(path: "test/files/sample archive.omex",
                mainFile: true, hidden: false, userSubmitted: true)
        def revision = new RevisionTransportCommand(format: omexFormat, files: [file])

        assertEquals([], omexService.getPubMedAnnotation(revision))
    }
}
