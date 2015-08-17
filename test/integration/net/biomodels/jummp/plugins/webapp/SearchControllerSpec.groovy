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
* Spring Framework, Spring Security (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Spring Framework, Spring Security used as well as
* that of the covered work.}
**/

package net.biomodels.jummp.plugins.webapp

import net.biomodels.jummp.core.model.ModelFormatTransportCommand
import net.biomodels.jummp.core.model.ModelTransportCommand as MTC
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.plugins.git.GitManagerFactory
import net.biomodels.jummp.plugins.security.Person
import net.biomodels.jummp.plugins.security.Role
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.plugins.security.UserRole
import net.biomodels.jummp.webapp.SearchController
import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclSid
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.GrantedAuthorityImpl
import org.springframework.security.core.context.SecurityContextHolder

import grails.test.mixin.TestFor
import grails.test.spock.IntegrationSpec
import spock.lang.Ignore
import spock.lang.FailsWith
import spock.lang.Shared

@TestFor(SearchController)
@Ignore
class SearchControllerSpec extends IntegrationSpec {
    def modelService
    def grailsApplication
    def springSecurityService
    def authenticationManager = grailsApplication.mainContext.getBean("authenticationManager")
    static Model testModel
    static List<Model> modelList

    def setupSpec() {
        createUserAndRoles()
        setupVcs()
        def auth = authenticate("testuser", "secret")
        final int COUNT = 51
        modelList = createDuplicateModels(COUNT, auth)
        assert COUNT == modelList.size()
        testModel = createModel("PharmML", "Test",
                "jummp-plugins/jummp-plugin-pharmml/test/files/0.2.1/example2.xml", null)
        assert testModel != null
        assert COUNT + 1 == Model.count()
    }

    def cleanupSpec() { FileUtils.deleteQuietly(new File("target/mockVcs/")) }

    void "list is the default method"() {
        when:
            controller.index()
        then:
            response.redirectedUrl == "/models"
    }

    /*
     * <p>These tests could be more DRY, but the result of the controller's action, or its
     * params map cannot be changed multiple times in the same test case.</p>
     *
     * <p>This means that the following example fails:</p>
     * <pre>
     *      controller.request.parameters = [foo: "bar"]
     *      controller.doSomething()
     *      controller.request.parameters = [foo: "baz"]
     *      assert controller.request.parameters["foo"] != "bar"
     *      assert controller.request.parameters["foo"] == "baz"
     * </pre>
     */
    void "list returns 10 results at a time if no preferences have been set"() {
        when:
            def auth = authenticate("testuser", "secret")
            def model = controller.list()
        then:
            model.models.size() == 10
    }

    void "list respects the offset parameter"() {
        when:
            String offset = "${Model.count() - 5}"
            controller.request.parameters = [offset: offset]
            def model = controller.list()
        then:
            model.models.size() == 5
    }

    void "list should sanitize input"() {
        when:
            controller.request.parameters = [offset: "abc"]
            def model = controller.list()
        then:
            model != null
            model.models.size() == 10
    }

    void "list ignores incorrect sort criteria"() {
        when:
            def crt = "popularity"
            def comparator = { a, b -> a.id > b.id }
            controller.request.parameters = [sortBy: crt]
            def model = controller.list()
        then:
            // bogus criteria result in sorting by model id
            doPairwiseComparisons(model.models, comparator)
            assert model.sortBy == "modified"
    }

    void "list can sort models by name in desc order"() {
        when:
            def crt = "name"
            controller.request.parameters = [sortBy: crt]
            def comparator = { a, b -> a.name >= b.name }
            def model = controller.list()
        then:
            testSortingBy(model, crt, comparator)
    }

    void "list can sort models by name in asc order"() {
        when:
            def crt = "name"
            controller.request.parameters = [sortBy: crt, sortDir: "asc"]
            def model = controller.list()
            def comparator = { a, b -> a.name <= b.name }
        then:
            assert model.sortDirection == "asc"
            testSortingBy(model, crt, comparator)
    }

    void "list can sort models by submitter in desc order"() {
        when:
            def crt = "submitter"
            controller.request.parameters = [sortBy: crt]
            def model = controller.list()
            def comparator = { a, b -> a.submitter >= b.submitter }
        then:
            testSortingBy(model, crt, comparator)
    }

    void "list can sort models by submitter in asc order"() {
        when:
            def crt = "submitter"
            controller.request.parameters = [sortBy: crt, sortDir: "asc"]
            def model = controller.list()
            def comparator = { a, b -> a.submitter <= b.submitter }
        then:
            assert model.sortDirection == "asc"
            testSortingBy(model, crt, comparator)
    }

    void "list can sort models by submission date in desc order"() {
        when:
            def crt = "submitted"
            controller.request.parameters = [sortBy: crt]
            def model = controller.list()
            def comparator = { a, b -> a.submissionDate >= b.submissionDate }
        then:
            testSortingBy(model, crt, comparator)
    }

    void "list can sort models by submission date in asc order"() {
        when:
            def crt = "submitted"
            controller.request.parameters = [sortBy: crt, sortDir: "asc"]
            def model = controller.list()
            def comparator = { a, b -> a.submissionDate <= b.submissionDate }
        then:
            assert model.sortDirection == "asc"
            testSortingBy(model, crt, comparator)
    }

    void "list can sort models by modification date in desc order"() {
        when:
            def crt = "modified"
            controller.request.parameters = [sortBy: crt]
            def model = controller.list()
            def comparator = { a, b -> a.lastModifiedDate >= b.lastModifiedDate }
        then:
            testSortingBy(model, crt, comparator)
    }

    void "list can sort models by modification date in asc order"() {
        when:
            def crt = "modified"
            controller.request.parameters = [sortBy: crt, sortDir: "asc"]
            def model = controller.list()
            def comparator = { a, b -> a.lastModifiedDate <= b.lastModifiedDate }
        then:
            assert model.sortDirection == "asc"
            testSortingBy(model, crt, comparator)
    }

    void testSortingBy(Map model, String criterion, Closure comparator) {
        doPairwiseComparisons(model.models, comparator)
        assert model.sortBy == criterion
    }

    void doPairwiseComparisons(List models, Closure comparator) {
        int limit = models.size() - 2
        (0..limit).each { i ->
            int j = i + 1
            MTC m1 = models[i]
            MTC m2 = models[j]
            assert compareModels(m1, m2, comparator)
        }
    }

    boolean compareModels(MTC m1, MTC m2, Closure comparison) { comparison.call(m1, m2) }

    private void createUserAndRoles() {
        assert springSecurityService != null
        User user, user2, admin, curator
        Person person
        if (!User.findByUsername("testuser")) {
            person = new Person(userRealName: "Test")
            user = new User(username: "testuser",
                    password: springSecurityService.encodePassword("secret"),
                    person: person,
                    email: "test@test.com",
                    enabled: true,
                    accountExpired: false,
                    accountLocked: false,
                    passwordExpired: false)
            assert person.save(flush:true, failOnError:true) != null
            assert user.save() != null
            assert new AclSid(sid: user.username, principal: true).save(flush: true) != null
        } else {
            user = User.findByUsername("testuser")
        }
        if (!User.findByUsername("username")) {
            person = new Person(userRealName: "Test2")
            user2 = new User(username: "username",
                    password: springSecurityService.encodePassword("verysecret"),
                    person: person,
                    email: "test2@test.com",
                    enabled: true,
                    accountExpired: false,
                    accountLocked: false,
                    passwordExpired: false)
            assert person.save(flush:true, failOnError:true) != null
            assert user2.save() != null
            def sid = new AclSid(sid: user2.username, principal: true).save(flush: true)
            assert sid != null
        } else {
            user2 = User.findByUsername("username")
        }
        if (!User.findByUsername("admin")) {
            person = new Person(userRealName: "administrator")
            admin = new User(username: "admin",
                    password: springSecurityService.encodePassword("1234"),
                    person: person,
                    email: "admin@test.com",
                    enabled: true,
                    accountExpired: false,
                    accountLocked: false,
                    passwordExpired: false)
            assert person.save(flush:true, failOnError:true) != null
            assert admin.save() != null
            def acl_sid = new AclSid(sid: admin.username, principal: true).save(flush: true)
            assert acl_sid != null
        } else {
            admin = User.findByUsername("admin")
        }
        if (!User.findByUsername("curator")) {
            person = new Person(userRealName: "Curator")
            assert person.save(flush:true, failOnError:true) != null
            curator = new User(username: "curator",
                    password: springSecurityService.encodePassword("extremelysecret"),
                    person: person,
                    email: "curator@test.com",
                    enabled: true,
                    accountExpired: false,
                    accountLocked: false,
                    passwordExpired: false)
            assert curator.save() != null
            assert new AclSid(sid: curator.username, principal: true).save(flush: true) != null
        } else {
            curator = User.findByUsername("curator")
        }
        ensureRoleExists("ROLE_USER")
        Role userRole = Role.findByAuthority("ROLE_USER")
        UserRole.create(user, userRole, false)
        UserRole.create(user2, userRole, false)
        UserRole.create(admin, userRole, false)
        UserRole.create(curator, userRole, false)
        ensureRoleExists("ROLE_ADMIN")
        Role adminRole = Role.findByAuthority("ROLE_ADMIN")
        UserRole.create(admin, adminRole, false)
        ensureRoleExists("ROLE_CURATOR")
        Role curatorRole = Role.findByAuthority("ROLE_CURATOR")
        UserRole.create(curator, curatorRole, false)
    }

    private def ensureRoleExists(String _authority) {
        if (!Role.findByAuthority(_authority)) {
            new Role(authority: _authority).save()
        }
    }

    private void setupVcs() {
        // setup VCS
        File clone = new File("target/mockVcs/wd/")
        clone.mkdirs()
        FileRepositoryBuilder builder = new FileRepositoryBuilder()
        Repository repository = builder.setWorkTree(clone)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir(clone) // scan up the file system tree
                .build()
        Git git = new Git(repository)
        git.init().setDirectory(clone).call()
        GitManagerFactory gitService = new GitManagerFactory()
        gitService.grailsApplication = grailsApplication
        grailsApplication.config.jummp.plugins.git.enabled = true
        grailsApplication.config.jummp.vcs.workingDirectory = clone.path
        File exchangeDir = new File("target/mockVcs/ed/")
        exchangeDir.mkdirs()
        grailsApplication.config.jummp.vcs.exchangeDirectory = exchangeDir.path
        modelService.vcsService.vcsManager = gitService.getInstance()
        modelService.vcsService.vcsManager.exchangeDirectory = exchangeDir
    }

    def createModel(String formatId, String submitter, String filePath, def auth = null) {
        if (!auth) {
            auth = authenticate("testuser", "secret")
        }
        assert auth != null
        def format = new ModelFormatTransportCommand(identifier: formatId)
        def model = new MTC(name: "My model", format: format, submissionId: "M1",
                comment: "Import my model.")
        def file = new RepositoryFileTransportCommand(mainFile: true, path: filePath,
               userSubmitted: true, description: "what a wonderful model")
        return modelService.uploadModelAsList([file], model)
    }

    def createDuplicateModels(int size, def auth = null) {
        def models = []
        auth = auth ?: authenticate("testuser", "secret")
        if (size > 0) {
            size.times {
                models << createModel("PharmML", "Test",
                        "jummp-plugins/jummp-plugin-pharmml/test/files/0.2.1/example2.xml", auth)
            }
        }
        assert models.size() == size
        return models
    }

    def authenticate(String username, String password) {
        def authToken = new UsernamePasswordAuthenticationToken(username, password)
        def auth = authenticationManager.authenticate(authToken)
        SecurityContextHolder.getContext().setAuthentication(auth)
        return auth
    }

    def anonymousAuthentication() {
        def auth = new AnonymousAuthenticationToken("test", "Anonymous",
                [ new GrantedAuthorityImpl("ROLE_ANONYMOUS")])
        SecurityContextHolder.getContext().setAuthentication(auth)
    }
}
