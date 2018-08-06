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
 * groovy, Apache Commons, Spring Framework, Grails, JUnit
 * (or a modified version of that library), containing parts covered by the terms
 * of Common Public License, Apache License v2.0, the licensors of this
 * Program grant you additional permission to convey the resulting work.
 * {Corresponding Source for a non-source form of such a combination shall
 * include the source code for the parts of groovy, Apache Commons,
 * Spring Framework, Grails, JUnit used as well as that of the covered work.}
 **/

package net.biomodels.jummp.plugins.webapp.submissionflow


import net.biomodels.jummp.core.model.ModelFormatTransportCommand
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand as RTC
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.plugins.webapp.SubmissionFlowTestBase
import org.apache.commons.io.FileUtils
import org.junit.After
import org.junit.Before
import org.junit.Test


public class SubmissionFlowUpdateTest extends SubmissionFlowTestBase {

    
    @After
    void tearDown() {
        super.tearDown()
    }

    @Before
    void setUp() {
    	initialise(false);
    }

    @Test
    void testUpdate() {
    	ModelTransportCommand meta = new ModelTransportCommand(comment: "Test Comment",
                name: "test", format: new ModelFormatTransportCommand(identifier: "UNKNOWN"),
                submissionId: "M123")
        File importFile = new File("target/vcs/exchange/import.xml")
        FileUtils.touch(importFile)
        importFile.append("Test\n")
        def rf = new RepositoryFileTransportCommand(path: importFile.absolutePath, description: "",
                mainFile: true)
        Model uploadedModel = modelService.uploadModelAsFile(rf, meta)
        assertTrue(uploadedModel.validate())
    	testSetup(uploadedModel.submissionId)
        assertFlowState("uploadFiles")
        File newFile = new File("jummp-plugins/jummp-plugin-pharmml/test/files/0.2.1/example1.xml")
        Map<File,String> additionalFiles = getRandomAdditionalFiles(10)
        addSubmissionFiles([newFile], additionalFiles)
        signalEvent("Upload")

        assertFlowState("displayModelInfo")
        signalEvent("Continue")
        assert true == (Boolean) flowScope.
                                    workingMemory.
                                    get("isUpdateOnExistingModel")
        assert "PharmML" == flowScope.workingMemory.get("model_type").identifier
        RTC revision=flowScope.workingMemory.get("RevisionTC") as RTC
        //test name
        assert "Example 1 - simulation continuous PK/PD" == revision.name
        
        //add tests for when displayModelInfo does something interesting
        //signalEvent("Continue") display model info disabled

        //Dont add publication info
        signalEvent("Continue")

        assertFlowState("displaySummaryOfChanges")
        Model model=modelService.getModel(uploadedModel.submissionId)
        Revision prev=modelService.getLatestRevision(model)
        assert prev
        signalEvent("Continue")
        //assert flowExecutionOutcome.id == "displayConfirmationPage"

        //test that the model is in fact saved in the database
        Revision rev=modelService.getLatestRevision(model)
        //test that revision is saved correctly
        assert rev
        assert rev.comment.contains("Model revised without commit message")
        assert rev.revisionNumber==prev.revisionNumber+1

        //test that files are updated in the repository correctly
        List<RepositoryFileTransportCommand> files = modelService.retrieveModelFiles(model)
        validateFiles(files, [importFile, newFile]+additionalFiles.keySet())

    }

}
