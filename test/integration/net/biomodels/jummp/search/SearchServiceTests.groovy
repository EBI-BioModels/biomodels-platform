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
 * A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along
 * with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
 *
 * Additional permission under GNU Affero GPL version 3 section 7
 *
 * If you modify Jummp, or any covered work, by linking or combining it with Apache Commons,
 * JUnit (or a modified version of that library), containing parts covered by the terms of
 * Common Public License, Apache License v2.0, the licensors of this Program grant you
 * additional permission to convey the resulting work.
 * {Corresponding Source for a non-source form of such a combination shall
 * include the source code for the parts of Apache Commons, JUnit used as well as
 * that of the covered work.}
 **/

package net.biomodels.jummp.search

import grails.test.spock.IntegrationSpec
import net.biomodels.jummp.core.JummpIntegrationTest
import net.biomodels.jummp.core.model.ModelFormatTransportCommand
import net.biomodels.jummp.core.model.ModelState
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.plugins.git.GitManagerFactory
import org.apache.commons.io.FileUtils
import org.springframework.security.acls.domain.BasePermission

/**
 * @author  tung.nguyen@ebi.ac.uk
 * @author  mihai.glont@ebi.ac.uk
 * @date    08/11/16.
 */
class SearchServiceTests extends IntegrationSpec {
    def aclUtilService
    def searchService
    def modelService
    def fileSystemService
    def grailsApplication
    def jummpIntegrationTest

    def setup() {
        jummpIntegrationTest = new JummpIntegrationTest()
        jummpIntegrationTest.createUserAndRoles()
        jummpIntegrationTest.authenticateAsTestUser()
    }

    def cleanup() {

    }

    def "search strategy should be loaded at least"() {
        given:
        def strategy = searchService.strategy
        expect:
        strategy != null
    }

    def "search should return empty set due to no public model available"() {
        given:
        int offset = 10
        int length = 20
        int facetCount = 10
        Map<String, Integer> paginationCriteria = ["start": offset, "length": length, "facetCount": facetCount]
        String query = "health"
        String domain = "biomodels"
        SortOrder sortOrder = new SortOrder()
        when:
        SearchResponse response = searchService.searchModels(query, domain, sortOrder, paginationCriteria)
        then:
        response.results.isEmpty()
        response.facets.isEmpty()
        response.totalCount == 0
    }

    def "search should return some results"() {
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
            smallModel("importModel.xml", nameTag, descriptionTag).absolutePath,
            mainFile: true, description: "This is a sample model")
        Model model = modelService.uploadModelAsFile(rf, new ModelTransportCommand(format:
            new ModelFormatTransportCommand(identifier: "PharmML"), comment: "test", name: "Test"))
        //wait a bit for the model to be indexed
        Thread.sleep(25000)

        // Make this model public
        Revision revision = model.revisions.last()
        model.firstPublished = new Date()
        aclUtilService.addPermission(revision, "ROLE_USER", BasePermission.READ)
        aclUtilService.addPermission(revision, "ROLE_ANONYMOUS", BasePermission.READ)
        revision.state = ModelState.PUBLISHED
        model.save(flush: true)
        // Search for the model using the name and description, and ensure it's the same we uploaded

        int offset = 10
        int length = 20
        int facetCount = 10
        Map<String, Integer> paginationCriteria = ["start": offset, "length": length, "facetCount": facetCount]
        String query = "health"
        String domain = "biomodels"
        SortOrder sortOrder = new SortOrder()
        when:
        SearchResponse response = searchService.searchModels(query, domain, sortOrder, paginationCriteria)
        then:
        !response.results.isEmpty()
        !response.facets.isEmpty()
        response.totalCount > 0
    }

    def "use to test draft cases"() {
        given: 'something'
        def x = 123
        when: 'a method is called'

        then: 'check something'
    }

    // Convenience functions follow..
    private File smallModel(String filename, String id, String desc) {
        return new File("test/files/test.xml")
    }

    private File getFileForTest(String filename, String text) {
        def tempDir = FileUtils.getTempDirectory()
        def testFile = new File(tempDir.absolutePath + File.separator + filename)
        if (text) {
            testFile.setText(text)
        }
        return testFile
    }
}
