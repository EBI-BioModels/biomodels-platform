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
* JGit, Apache Commons, JUnit (or a modified version of that library), containing parts
* covered by the terms of Common Public License, Apache License v2.0, Eclipse Distribution License v1.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of JGit, Apache Commons, JUnit used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.plugins

import com.ctc.wstx.api.ReaderConfig
import com.ctc.wstx.stax.WstxInputFactory
import net.biomodels.jummp.core.adapters.DomainAdapter 
import net.biomodels.jummp.core.JummpIntegrationTest
import net.biomodels.jummp.core.model.ModelFormatTransportCommand
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.ModelFormat
import net.biomodels.jummp.plugins.git.GitManagerFactory
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.junit.*
import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin
import javax.xml.stream.XMLInputFactory
import static org.junit.Assert.*
/**
 * Test for SbmlService parts which require a running core to retrieve Models.
 */
@TestMixin(IntegrationTestMixin)
class SbmlServiceTests extends JummpIntegrationTest {
    /**
     * Dependency injection of SbmlService
     */
    def sbmlService
    def modelService
    def fileSystemService
    def grailsApplication

    @Before
    void setUp() {
        createUserAndRoles()
        setupVcs()
        // disable validation as it is broken
        grailsApplication.config.jummp.plugins.sbml.validation = false
    }

    @After
    void tearDown() {
        FileUtils.deleteDirectory(new File("target/sbml"))
    }

    @Test
    void testExtractName() {
        File myModel = null
        assertEquals("", sbmlService.extractName([myModel]))
        myModel = new File("target/sbml/myModel")
        FileUtils.deleteQuietly(myModel)
        assertEquals("", sbmlService.extractName([myModel]))
        assertEquals("", sbmlService.extractName([myModel, myModel]))
        myModel = new File("test/files/BIOMD0000000272.xml")
        assertEquals("Becker2010_EpoR_AuxiliaryModel", sbmlService.extractName([myModel]))
    }

    @Test
    void testAreFilesThisFormat() {
        File file = new File("target/sbml/test")
        FileUtils.deleteQuietly(file)
        FileUtils.touch(file)
        assertFalse(sbmlService.areFilesThisFormat([file]))

        // unknown sbml
        File unknown = new File("target/sbml/unknown")
        FileUtils.deleteQuietly(unknown)
        FileUtils.touch(unknown)
        unknown.append('''\
<?xml version='1.0' encoding='UTF-8'?>
<model/>''')
        assertFalse(sbmlService.areFilesThisFormat([unknown]))

        File properSbml = new File("test/files/BIOMD0000000272.xml")
        assertTrue(sbmlService.areFilesThisFormat([properSbml]))

        assertTrue(sbmlService.areFilesThisFormat([properSbml,smallModel("validSbml.xml")]))
        assertFalse(sbmlService.areFilesThisFormat([properSbml, unknown]))
    }

    /*
     * Ensure both XMLInputFactory.newInstance() and new WstxInputFactory() yield a factory
     * that does not report whitespaces in prologue.
     *
     * The setting WstxInputFactory.CFG_REPORT_PROLOG_WS is considered off if
     *     factory.getConfig().getConfigFlags() & WstxInputFactory.CFG_REPORT_PROLOG_WS == 0
     * To switch off this setting manually, one would use
     *     factory.setProperty(XMLInputFactory2.P_REPORT_PROLOG_WHITESPACE, Boolean.FALSE)
     *
     * Having this setting enabled messes up the jsbml parsing algorithm.
     */
    @Test
    void checkWstxReaderConfiguration() {
        ReaderConfig defaultConfig = ReaderConfig.createFullDefaults()
        int defaultFlags = defaultConfig.getConfigFlags()

        XMLInputFactory genericFactory = XMLInputFactory.newInstance()
        assertTrue(genericFactory instanceof WstxInputFactory)
        ReaderConfig cfg = genericFactory.getConfig()
        int flags = cfg.getConfigFlags()
        int propertyCheck = flags & WstxInputFactory.CFG_REPORT_PROLOG_WS
        assertEquals(0, propertyCheck)
        assertEquals(defaultFlags, flags)

        WstxInputFactory factory = new WstxInputFactory()
        cfg = factory.getConfig()
        flags = cfg.getConfigFlags()
        propertyCheck = flags & WstxInputFactory.CFG_REPORT_PROLOG_WS
        assertEquals(0, propertyCheck)
        assertEquals(defaultFlags, flags)
    }


    @Test
    void testLevelAndVersion() {
        authenticateAsTestUser()
        def rf = new RepositoryFileTransportCommand(path: smallModel("BIOMD0000000272.xml").absolutePath,
                    description: "", mainFile: true)
        Model model = modelService.uploadModelAsFile(rf, new ModelTransportCommand(format:
                new ModelFormatTransportCommand(identifier: "SBML"), comment: "test", name: "Test"))
        RevisionTransportCommand rev = DomainAdapter
                                        .getAdapter(modelService
                                                    .getLatestRevision(model))
                                                    .toCommandObject()
        assertEquals(1, sbmlService.getLevel(rev))
        assertEquals(1, sbmlService.getVersion(rev))
        assertEquals("L1V1", sbmlService.getFormatVersion(rev))
        rf.path = "test/files/BIOMD0000000272.xml"
        RevisionTransportCommand rev2 = DomainAdapter.getAdapter(modelService.addRevisionAsFile(model, rf,
                ModelFormat.findByIdentifierAndFormatVersion("SBML", "L2V4"),"test")).toCommandObject()
        assertEquals(2, sbmlService.getLevel(rev2))
        assertEquals(4, sbmlService.getVersion(rev2))
        assertEquals("L2V4", sbmlService.getFormatVersion(rev2))
    }

    @Test
    void testModelMetaId() {
        authenticateAsTestUser()
        def rf = new RepositoryFileTransportCommand(path: smallModel("BIOMD0000000272.xml"), mainFile:true, description: "")
        Model model = modelService.uploadModelAsFile(rf, new ModelTransportCommand(format:
                new ModelFormatTransportCommand(identifier: "SBML"), comment: "test", name: "Test"))
        RevisionTransportCommand rev = DomainAdapter
                                        .getAdapter(modelService.getLatestRevision(model))
                                        .toCommandObject()
        assertEquals("", sbmlService.getMetaId(rev))
        rf.path = "test/files/BIOMD0000000272.xml"
        RevisionTransportCommand rev2 = DomainAdapter.getAdapter(modelService.addRevisionAsFile(model, rf,
                ModelFormat.findByIdentifierAndFormatVersion("SBML", "L2V4"), "test")).toCommandObject()
        assertEquals("_688624", sbmlService.getMetaId(rev2))
    }

    private File getFileForTest(String filename, String text)
    {
        def tempDir = FileUtils.getTempDirectory()
        def testFile = new File(tempDir.absolutePath + File.separator + filename)
        if (text) {
            testFile.setText(text)
        }
        return testFile
    }

    @Test
    void testModelNotes() {
        authenticateAsTestUser()
        def rf = new RepositoryFileTransportCommand(path: smallModel("testModelNotes.xml").absolutePath, mainFile:true, description: "")
        Model model = modelService.uploadModelAsFile(rf, new ModelTransportCommand(format: 
                new ModelFormatTransportCommand(identifier: "SBML"), comment: "test", name: "Test"))
        RevisionTransportCommand rev = DomainAdapter.getAdapter(modelService.getLatestRevision(model)).toCommandObject()
        assertEquals("", sbmlService.getNotes(rev))

        File modelWithNotes = getFileForTest("testModelNotes.xml",'''<?xml version="1.0" encoding="UTF-8"?>
<sbml xmlns="http://www.sbml.org/sbml/level1" level="1" version="1">
  <model>
    <notes><body xmlns="http://www.w3.org/1999/xhtml"><p>Test</p></body></notes>
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
        rf.path = modelWithNotes.absolutePath
        RevisionTransportCommand rev2 = DomainAdapter.getAdapter(modelService.addRevisionAsFile(model, rf, 
                ModelFormat.findByIdentifierAndFormatVersion("SBML", "L1V1"), "test")).toCommandObject()
        String notes  = sbmlService.getNotes(rev2);
        assertTrue(notes.contains("<notes>"));
        assertTrue(notes.contains("http://www.w3.org/1999/xhtml"));
        assertTrue(notes.contains("Test"));
        assertTrue(notes.contains("</body>"));
        assertTrue(notes.contains("</notes>"));
    }

    private void setupVcs() {
        fileSystemService.root = new File("target/sbml/git/").getCanonicalFile()
        fileSystemService.root.mkdirs()
        String containerPath = fileSystemService.root.absolutePath + "/sss/"
        fileSystemService.currentModelContainer = containerPath
        modelService.vcsService.modelContainerRoot = fileSystemService.root
        GitManagerFactory gitService = new GitManagerFactory()
        gitService.grailsApplication = grailsApplication
        grailsApplication.config.jummp.plugins.git.enabled = true
        grailsApplication.config.jummp.vcs.workingDirectory = "target/sbml/git/"
        File exchangeDir = new File("target/sbml/exchange/")
        exchangeDir.mkdirs()
        grailsApplication.config.jummp.vcs.exchangeDirectory = exchangeDir.path
        modelService.vcsService.vcsManager = gitService.getInstance()
        modelService.vcsService.vcsManager.exchangeDirectory = exchangeDir
    }

    private File smallModel(String filename) {
        return getFileForTest(filename, '''\
<?xml version="1.0" encoding="UTF-8"?>
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
    }
}
