/**
 * Copyright (C) 2010-2019 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

import grails.test.runtime.FreshRuntime
import grails.test.spock.IntegrationSpec
import net.biomodels.jummp.core.model.*
import net.biomodels.jummp.core.util.JummpXmlUtils
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.Publication
import net.biomodels.jummp.model.PublicationPerson
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.plugins.security.Person
import net.biomodels.jummp.core.user.PersonTransportCommand
import net.biomodels.jummp.plugins.security.User
import org.apache.commons.io.FileUtils
import org.springframework.transaction.TransactionDefinition

@FreshRuntime
class PublicationServiceSpec extends IntegrationSpec {
    def fileSystemService
    def grailsApplication
    def jummpIntegrationTest
    def miriamService
    def messageSource
    def modelDelegateService
    def modelService
    def publicationService
    def pubMedService
    def vcsService

    def setup() {
        jummpIntegrationTest = new JummpIntegrationTest()
    }

    def cleanup() {
        File wd = new File("target/vcs/wd")
        File ed = new File("target/vcs/ed")
        if (wd.exists()) {
            FileUtils.deleteDirectory(wd)
        }
        if (ed.exists()) {
            FileUtils.deleteDirectory(ed)
        }
    }

    /**
     * The test context: the database has a person whose the orcid identifier was persisted.
     * We are trying to fetch a PubMed publication which one of the authors is that person.
     * The test passes if the person could be reused instead created.
     */
    def "test fetch a publication which one of the authors has ORCID persisted in the database"() {
        given: "have a PubMed ID"
        String pubMedID = "23664840"

        when: "try to fetch a publication which one of the authors has ORCID persisted in the database"
        Person zanghellini = new Person(userRealName: "Zanghellini J", orcid: "0000-0002-1964-2455")
        assert null != zanghellini
        zanghellini.save()
        assert 1 == Person.count()
        PublicationTransportCommand ptc = pubMedService.fetchPublicationData(pubMedID)
        assert null != ptc
        // the following statement will persist authors into the database
        publicationService.fromCommandObject(ptc)

        then: "author named Zanghellini J will be reused rather than newly created"
        ptc.authors.size() == 3
        Person.count() == 3
    }

    /**
     * The test context: @fetchPublicationData parses and saves authors into database. This test is querying
     * these authors and comparing them with  the expected values.
     */
    def "test PublicationPerson after fetching from PubMed Central"() {
        given: "have a PubMed ID"
        String pubMedID = "25414348" // BioModels: ten-year anniversary
        PublicationPerson viji
        when: "fetch that publication from PubMed Central"
        assert 0 == Person.count()
        assert 0 == PublicationPerson.count()
        PublicationTransportCommand ptc = pubMedService.fetchPublicationData(pubMedID)
        Publication publication = publicationService.fromCommandObject(ptc)
        assert 20 == PublicationPerson.count()
        viji = PublicationPerson.findByPubAlias("Chelliah V")
        List pp = PublicationPerson.findAllByPublication(publication)
        then: "publication authors should be persisted"
        null != publication
        20 == pp?.size()
        viji.position == 0 // the position of the first author is counted from 0
        viji.pubAlias == "Chelliah V"
    }
    /**
     * The test context: if the authors of a publication transport command object are wrongly modified, the publication
     * in question should not be valid.
     */
    def "test any of the authors is invalid"() {
        given: "a PubMed ID"
        String pubMedID = "25414348" // BioModels: ten-year anniversary

        when: "fetch that publication from PubMed Central"
        assert 0 == Person.count()
        assert 0 == PublicationPerson.count()
        PublicationTransportCommand ptc = pubMedService.fetchPublicationData(pubMedID)

        then: "publication authors should be extracted properly"
        ptc.authors.size() == 20

        when: "attempt to corrupt any of the authors"
        PersonTransportCommand firstAuthor = ptc.authors.first()
        firstAuthor.userRealName = null

        then: "the authors as well as publication will not be validated"
        !ptc.validate()
        ptc.errors.getFieldErrors("authors").size() == 1
    }

    /**
     * The test context: testing assembleAuthors methods with different authors JSON strings as the input/argument.
     * The method will be failed if any of the authors in the JSON string has a nullable real user name or an
     * invalid orcid identifier.
     */
    def "test assembleAuthors method"() {
        given: "null PublicationTransportCommand instance"
        PublicationLinkProviderTransportCommand linkProviderTC = pubMedService.createLinkProviderInstance()
        PublicationTransportCommand ptc = null

        when: "instantiate the given publication transport command object with a map of parameters without authors"
        Map args = [title: "BioModels repository - versioning models",
                    journal: "Software Engineering Journal",
                    synopsis: "this is a online repository of biological mathematics models",
                    affiliation: "EMBL-EBI", pages: "1-10",
                    year: 2019,
                    month: "January",
                    linkProvider: linkProviderTC,
                    link: "14567897"]
        ptc = new PublicationTransportCommand(args)
        then: "the instance could not be valid because of missing authors"
        // The validation of this ptc returns false because of missing authors
        !ptc.validate()

        when: "assemble the authors parsed from a JSON string into the publication"
        def invalidAuthorsInJSONString = """\
{"authors":[{"userRealName":"Abroudi A","institution":"","orcid":""},{"userRealName":"","institution":"","orcid":"0000-0003-2943-4331"},{"userRealName":"Kulasiri D","institution":"","orcid":"0000#0001#8744#1578"}]}"""
        publicationService.assembleAuthors(ptc, invalidAuthorsInJSONString)
        then: "publication is till invalid because of invalid JSON string has an empty real user name and an invalid orcid. The method will throw an exception"
        !ptc.validate() // because there are two bad authors which one has no real name and the other has an invalid
        // orcid
        thrown(InvalidPublicationAuthorsException)

        when: "assemble again the authors parsed from an invalid JSON string into the publication"
        def badAuthorsInJSONString = """\
{"authors":[{"userRealName":"Abroudi A","institution":"","orcid":""},{"userRealName":"","institution":"","orcid":"0000-0003-2943-4331"},{"userRealName":"Kulasiri D","institution":"","orcid":"0000-0001-8744-1578"}]}"""
        publicationService.assembleAuthors(ptc, badAuthorsInJSONString)
        !ptc.validate() // due to there is a bad authors which has no real name
        then: "the method will throw an exception and we hope to capture error messages"
        final InvalidPublicationAuthorsException exception = thrown()
        String errorMessage = exception.getI18nErrorMessage4InvalidAuthor()
        String i18nMessage = messageSource.getMessage("personTransportCommand.userRealName.blank", [] as Object[], Locale.default)
        errorMessage.contains(i18nMessage)
        exception.invalidAuthors.size() == 1

        when: "assemble the authors extracted from a valid JSON string into the publication"
        def authorsInJSONString = """\
{"authors":[{"userRealName":"Abroudi A","institution":"","orcid":""},{"userRealName":"Samarasinghe S","institution":"","orcid":"0000-0003-2943-4331"},{"userRealName":"Kulasiri D","institution":"","orcid":"0000-0001-8744-1578"}]}"""
        publicationService.assembleAuthors(ptc, authorsInJSONString)
        then: "the publication is valid and the authors are successfully populated"
        ptc.validate()
        ptc.authors.size() == 3
    }

    /**
     * The test context: when the given model is attached a publication and this test aims at checking the association
     * between the model and the publication.
     */
    def "test Model-Publication association"() {
        setup: "accompanying components"
        // working and exchange directory
        File workingDir = new File("target/vcs/wd/ppp/")
        workingDir.mkdirs()
        File exchangeDir = new File("target/vcs/ed/")
        exchangeDir.mkdirs()
        String rootPath = workingDir.getParent()

        when: "initialise services' properties"
        grailsApplication.config.jummp.vcs.workingDirectory = rootPath
        grailsApplication.config.jummp.vcs.exchangeDirectory = exchangeDir.path
        String REGISTRY_EXPORT_FILE_NAME = "testMiriam.xml"
        miriamService.registryExport = new File(exchangeDir.path, REGISTRY_EXPORT_FILE_NAME)
        String currentContainer = workingDir.getCanonicalPath()
        fileSystemService.currentModelContainer.set(currentContainer)
        fileSystemService.root = workingDir.getParentFile()
        vcsService.modelContainerRoot = rootPath
        def gitFactory = grailsApplication.mainContext.getBean("gitManagerFactory")
        vcsService.vcsManager = gitFactory.getInstance()
        vcsService.vcsManager.exchangeDirectory = exchangeDir
        then: "they are successfully initialised"
        workingDir.exists()
        exchangeDir.exists()
        null != rootPath
        null != grailsApplication.config.jummp.vcs.workingDirectory
        vcsService.isValid()

        when: "create a user, authenticate him"
        /**
         * The model files will be uploaded in a dedicated transaction in which the hibernate couldn't see the user
         * owning the model. The user should exist in User table for looking up later. Thus, creating user and roles
         * must be in a dedicated transaction.
         */
        def txDefinition = [propagationBehavior: TransactionDefinition.PROPAGATION_REQUIRES_NEW]
        Model.withTransaction(txDefinition) {
            jummpIntegrationTest.createUserAndRoles()
        }
        jummpIntegrationTest.authenticateAsUser()
        then: "test the newly created user"
        User.findByUsername("username").username == "username"

        when: "create a model"
        File f = new File("test/files/BIOMD0000000272.xml")
        f.exists()
        String name = JummpXmlUtils.findModelAttribute(f, "model", "name").trim()
        null != name

        def rf = new RepositoryFileTransportCommand(path: f.absolutePath, description: "SBML representation", mainFile: true)
        def fmt = new ModelFormatTransportCommand(identifier: "SBML", formatVersion: "L2V4")
        final String pid = "22761472"

        PublicationTransportCommand publication = pubMedService.fetchPublicationData(pid)
        def mtc = new ModelTransportCommand(publication: publication)
        def rev = new RevisionTransportCommand(name: name, validated: true, format: fmt, model: mtc, owner: "username")
        Model m = modelService.uploadValidatedModel([rf], rev)
        Model.withSession { s ->
            s.flush()
            s.clear()
        }
        then: "the model was saved successfully"
        assert publication
        assert m.publication

        when: "load the model and publication"
        Model alterEgo = Model.load(m.id)
        Revision firstCommit = modelService.getLatestRevision(alterEgo, false)
        Publication alterPub = firstCommit.model.publication
        List<File> rfs = vcsService.retrieveFiles(firstCommit)
        RevisionTransportCommand rtc = modelDelegateService.getRevisionFromParams("${m.submissionId}", null)
        PublicationTransportCommand ptc = rtc.model.publication

        String journal = "Clinical cancer research : an official journal of the American Association for Cancer Research"
        String title = "A tumor growth inhibition model for low-grade glioma treated with chemotherapy or radiotherapy."
        String pages = "5071-5080"
        then: "test the publication is loaded fully"
        null != rtc
        null != ptc

        expect: "the publication was saved and its metadata is matched with the actual values"
        /**
         * Test Model's Files
         */
        1 == rfs.size()
        "BIOMD0000000272.xml" == rfs.first().name

        /**
         * Test Publication
         */
        null != firstCommit
        alterEgo.id == firstCommit.model.id
        assert alterPub
        journal == alterPub.journal
        title == alterPub.title
        alterPub.affiliation.length() > 0
        alterPub.synopsis.length() > 0
        pages == alterPub.pages

        /**
         * Test PublicationTransportCommand
         */
        null != rtc
        null != ptc
        journal == ptc.journal
        title == ptc.title
        pages == ptc.pages
    }
}
