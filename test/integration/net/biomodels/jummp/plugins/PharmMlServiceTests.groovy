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

import net.biomodels.jummp.core.VcsTestSupport
import net.biomodels.jummp.core.JummpIntegrationTest
import net.biomodels.jummp.core.adapters.RevisionAdapter
import net.biomodels.jummp.core.model.ModelFormatTransportCommand
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.ModelFormat
import net.biomodels.jummp.model.Revision
import org.apache.commons.io.FileUtils
import org.junit.*
import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin
import static org.junit.Assert.*

@TestMixin(IntegrationTestMixin)
class PharmMlServiceTests extends JummpIntegrationTest {

    def fileSystemService
    def modelService
    def pharmMlService
    def grailsApplication

    private File defaultRootModelDir
    private String defaultWorkingDir
    private String defaultExchangeDir
    private def defaultVcsManager

    @Before
    void setUp() {
        defaultRootModelDir = fileSystemService.root
        defaultWorkingDir   = grailsApplication.config.jummp.vcs.workingDirectory
        defaultExchangeDir  = grailsApplication.config.jummp.vcs.exchangeDirectory
        defaultVcsManager   = modelService.vcsService.vcsManager
        createUserAndRoles()
        setupVcs()
   }

    @After
    void tearDown() {
        fileSystemService.root = defaultRootModelDir
        grailsApplication.config.jummp.vcs.workingDirectory  = defaultWorkingDir
        grailsApplication.config.jummp.vcs.exchangeDirectory = defaultExchangeDir
        modelService.vcsService.vcsManager = defaultVcsManager
        FileUtils.deleteQuietly(new File("target/pharmml/"))
    }

    @Test
    void testAreFilesThisFormat() {
        assertFalse pharmMlService.areFilesThisFormat([null])
        def modelFile = new File("target/pharmml/exchange/unknown.txt")
        FileUtils.touch modelFile
        modelFile.setText("I am the main entry of this model submission.")
        assertFalse pharmMlService.areFilesThisFormat([modelFile])
        modelFile = new File("jummp-plugins/jummp-plugin-pharmml/test/files/0.2.1/example2.xml")
        assertTrue pharmMlService.areFilesThisFormat([modelFile])
        modelFile = new File("test/files/BIOMD0000000272.xml")
        assertFalse pharmMlService.areFilesThisFormat([modelFile])
        modelFile = new File("jummp-plugins/jummp-plugin-pharmml/test/files/0.6/timeToEvent_weibullHazard.xml")
        assertTrue pharmMlService.areFilesThisFormat([modelFile])
    }

    @Test
    void testGetVersionFormat() {
        def modelFile = new File("jummp-plugins/jummp-plugin-pharmml/test/files/0.6/timeToEvent_weibullHazard.xml")
        authenticateAsTestUser()
        def rf = new RepositoryFileTransportCommand(path: modelFile.absolutePath,
                    description: "A very interesting model.", mainFile: true)
        def defaultFormat = new ModelFormatTransportCommand(identifier: "PharmML")
        def modelCommand = new ModelTransportCommand(format: defaultFormat, comment: "First commit", name: "Foo")
        Model model = modelService.uploadModelAsFile(rf, modelCommand)
        def revision = new RevisionAdapter(revision: modelService.getLatestRevision(model)).toCommandObject()
        assertEquals "0.6", pharmMlService.getFormatVersion(revision)
    }

    @Test
    void testValidate() {
        def modelFile = new File("jummp-plugins/jummp-plugin-pharmml/test/files/0.6/timeToEvent_weibullHazard.xml")
        authenticateAsTestUser()
        def rf = new RepositoryFileTransportCommand(path: modelFile.absolutePath,
                    description: "A very interesting model.", mainFile: true)
        def defaultFormat = new ModelFormatTransportCommand(identifier: "PharmML")
        def modelCommand = new ModelTransportCommand(format: defaultFormat, comment: "First commit", name: "Foo")
        Model model = modelService.uploadModelAsFile(rf, modelCommand)
        def revision = new RevisionAdapter(revision: modelService.getLatestRevision(model)).toCommandObject()
        List<String> locations = []
        revision.files?.findAll{it.mainFile}.each { locations << it.path}
        List<File> files = []
        locations.each { l ->
            File f = new File(l)
            if (f && f.exists() && f.canRead()) {
                files << f
            }
        }
        assertFalse files.isEmpty()
        assertTrue pharmMlService.validate(files, [])
    }

    private void setupVcs() {
        VcsTestSupport.setupVcs(grailsApplication, modelService, fileSystemService, "target/pharmml", "aaa")
    }
}
