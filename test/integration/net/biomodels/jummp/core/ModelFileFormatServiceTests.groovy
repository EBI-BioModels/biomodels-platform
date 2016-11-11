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
* Apache Commons, JUnit (or a modified version of that library), containing parts
* covered by the terms of Common Public License, Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Apache Commons, JUnit used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.core

import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin
import net.biomodels.jummp.core.model.FileFormatService
import net.biomodels.jummp.core.model.ModelFormatTransportCommand
import net.biomodels.jummp.core.model.UnknownFormatService
import net.biomodels.jummp.model.ModelFormat
import net.biomodels.jummp.plugins.sbml.SbmlService
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand as RFTC
import org.apache.commons.io.FileUtils
import org.junit.*
import static org.junit.Assert.*

@TestMixin(IntegrationTestMixin)
class ModelFileFormatServiceTests {
    def modelFileFormatService
    def grailsApplication
    @Before
    void setUp() {
        FileUtils.deleteDirectory(new File("target/sbml"))
        assertTrue(new File("target/sbml").mkdirs())
    }

    @After
    void tearDown() {
        FileUtils.deleteDirectory(new File("target/sbml"))
        grailsApplication.config.jummp.plugins.sbml.validation = false
    }

    @Test
    void testServiceForFormat() {
        // unknown format should return null
        def dontKnowThisFormatService = modelFileFormatService.serviceForFormat(
                ModelFormat.findByIdentifier("UNKNOWN"))
        assertNotNull(dontKnowThisFormatService)
        // for sbml it needs to be a SbmlService
        def formatService = modelFileFormatService.serviceForFormat(
                ModelFormat.findByIdentifierAndFormatVersion("SBML", "*"))
        assertNotNull(formatService)
        assertTrue(formatService instanceof FileFormatService)
        assertTrue(formatService instanceof SbmlService)
        assertTrue(dontKnowThisFormatService instanceof UnknownFormatService)
    }

    @Test
    void testInfer() {
        // for null, there is no format
        assertNull(modelFileFormatService.inferModelFormat(null))

        // an unknown format file
        File validUnknown = new File("target/sbml/unknown")
        RFTC rftc = new RFTC(path: validUnknown.getCanonicalPath(), mainFile: true);
        validUnknown.setText('What is my name')
        assertEquals("UNKNOWN", modelFileFormatService.inferModelFormat([rftc]).identifier)

        // an SBML file should be detected. Make less restrictive to accept invalid SBML files
        File validSbml = new File("target/sbml/validSbml")
        validSbml.setText('''<?xml version="1.0" encoding="UTF-8"?>
<sbml xmlns="http://www.sbml.org/sbml/level1" level="1" version="1">
  <model>
    <listOfCompartments>
      <compartment name="x"/>
    </listOfCompartments>
    <listOfSpecies>
      <specie name="y" compartment="x" initialAmount="1"/>
    </listOfSpecies>
    <listOfReactions>
      <reaction name="r">
        <listOfReactants>
          <specieReference specie="y"/>
        </listOfReactants>
        <listOfProducts>
          <specieReference specie="y"/>
        </listOfProducts>
      </reaction>
    </listOfReactions>
  </model>
</sbml>''')
		rftc.path = validSbml.getCanonicalPath();
        assertEquals("SBML", modelFileFormatService.inferModelFormat([rftc]).identifier)
    }

    @Ignore("Don't test SBML validation because of interoperability issues between JSBML and libSBML.")
    @Test
    void testValidate() {
        grailsApplication.config.jummp.plugins.sbml.validation = true
        // for unknown file type this should evaluate to false
        assertFalse(modelFileFormatService.validate(null, ModelFormat.findByIdentifier("UNKNOWN")))

        // a valid unknown format file
        File validUnknown = new File("target/sbml/unknown")
        validUnknown.setText('What is my name')
        assertTrue(modelFileFormatService.validate([validUnknown], ModelFormat.findByIdentifier("UNKNOWN")))

        // for an invalid sbml file it should also evaluate to false
        File invalidSbml = new File("target/sbml/unknown")
        FileUtils.deleteQuietly(invalidSbml)
        FileUtils.touch(invalidSbml)
        invalidSbml.append('''<?xml version='1.0' encoding='UTF-8'?>
<sbml level="99" version="1">
  <model/>
</sbml>''')
        assertFalse(modelFileFormatService.validate([invalidSbml],
                ModelFormat.findByIdentifierAndFormatVersion("SBML", "*")))
        // and for a valid SBML file it should be true
        File validSbml = new File("target/sbml/validSbml")
        FileUtils.deleteQuietly(validSbml)
        FileUtils.touch(validSbml)
        validSbml.append('''<?xml version="1.0" encoding="UTF-8"?>
<sbml xmlns="http://www.sbml.org/sbml/level1" level="1" version="1">
  <model>
    <listOfCompartments>
      <compartment name="x"/>
    </listOfCompartments>
    <listOfSpecies>
      <specie name="y" compartment="x" initialAmount="1"/>
    </listOfSpecies>
    <listOfReactions>
      <reaction name="r">
        <listOfReactants>
          <specieReference specie="y"/>
        </listOfReactants>
        <listOfProducts>
          <specieReference specie="y"/>
        </listOfProducts>
      </reaction>
    </listOfReactions>
  </model>
</sbml>''')
        assertTrue(modelFileFormatService.validate([validSbml], ModelFormat.findByIdentifierAndFormatVersion("SBML", "L1V1"))) }

    @Test
    void testExtractName() {
        // for unknown format it's empty
        assertEquals("", modelFileFormatService.extractName(null, ModelFormat.findByIdentifier("UNKNOWN")))
        File sbmlModel = new File("test/files/BIOMD0000000272.xml")
        assertEquals("Becker2010_EpoR_AuxiliaryModel", modelFileFormatService.extractName([sbmlModel],
                ModelFormat.findByIdentifierAndFormatVersion("SBML", "*")))
        File omexModel = new File("jummp-plugins/jummp-plugin-combine-archive/test/files/sample.omex")
        assertEquals("", modelFileFormatService.extractName([omexModel],
                ModelFormat.findByIdentifierAndFormatVersion("OMEX", "*")))
    }
}
