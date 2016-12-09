/**
 * Copyright (C) 2010-2016 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
 **/





package net.biomodels.jummp.core

import grails.test.spock.IntegrationSpec
import net.biomodels.jummp.core.model.ModelFormatTransportCommand
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand
import net.biomodels.jummp.model.Flag
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.plugins.git.GitManagerFactory

/**
 * @author  tung.nguyen@ebi.ac.uk
 * @author  mihai.glont@ebi.ac.uk
 */
class ModelFlagServiceTests extends IntegrationSpec {
    def modelService
    def fileSystemService
    def grailsApplication
    def jummpIntegrationTest
    def modelFlagService

    def setup() {
        jummpIntegrationTest = new JummpIntegrationTest()
        jummpIntegrationTest.createUserAndRoles()
        jummpIntegrationTest.authenticateAsTestUser()
    }

    def cleanup() {

    }

    def "update flag for a given model"() {
        setup:
        // generate unique ids for the name and description
        String nameTag = "testModel"
        String descriptionTag = "test description"
        GitManagerFactory gitService = new GitManagerFactory()
        gitService.grailsApplication = grailsApplication
        grailsApplication.config.jummp.plugins.git.enabled = true
        grailsApplication.config.jummp.vcs.exchangeDirectory = "target/vcs/exchange"
        grailsApplication.config.jummp.vcs.workingDirectory = "target/vcs/git"
        grailsApplication.config.jummp.plugins.sbml.validation = false
        modelService.vcsService.vcsManager = gitService.getInstance()
        modelService.vcsService.vcsManager.exchangeDirectory = new File('target/vcs/exchange')
        // upload the model
        def rf = new RepositoryFileTransportCommand(path:
            new File("test/files/JUM-84/pharmml/testPharmML.xml").absolutePath,
            mainFile: true, description: "This is a sample model")
        Model model = modelService.uploadModelAsFile(rf, new ModelTransportCommand(format:
            new ModelFormatTransportCommand(identifier: "PharmML"), comment: "test", name: "Test"))
        //wait a bit for the model to be indexed
        Thread.sleep(25000)

        and: "a flag"
        String label = "This is a valid SBML model"
        String description = "This model is satisfied with SBML specification"
        def icon = [1, 1, 1, 1, 1, 1, 1, 1, 1] as byte[]
        Flag flag = modelFlagService.saveFlag(label, description, icon)
        when: "assign this flag to the model"
        def result = modelFlagService.flagModel(model, flag)
        then: "hope that operation is nice!"
        result != null
    }
}
