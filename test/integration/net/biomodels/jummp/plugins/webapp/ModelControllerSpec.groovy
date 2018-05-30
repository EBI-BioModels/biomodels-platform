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

import grails.util.Holders as H
import net.biomodels.jummp.core.model.ModelFormatTransportCommand
import net.biomodels.jummp.core.model.ModelTransportCommand as MTC
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.plugins.git.GitManagerFactory
import net.biomodels.jummp.plugins.security.Person
import net.biomodels.jummp.plugins.security.Role
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.plugins.security.UserRole
import net.biomodels.jummp.webapp.ModelController
import org.apache.commons.io.FileUtils
import grails.plugin.springsecurity.acl.AclSid
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.GrantedAuthorityImpl
import org.springframework.security.core.context.SecurityContextHolder

import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin
import spock.lang.*

@TestMixin(IntegrationTestMixin)
@TestFor(ModelController)
@Ignore
class ModelControllerSpec extends Specification {
    def modelService
    @Shared def grailsApplication = H.grailsApplication
    @Shared def springSecurityService = H.applicationContext.getBean("springSecurityService")
    @Shared def authenticationManager = H.applicationContext.getBean("authenticationManager")

    def setupSpec() {
        createUserAndRoles()
        setupVcs()
        def auth = authenticate("testuser", "secret")
        final int COUNT = 10
        createDuplicateModels(COUNT, auth)
    }

    def cleanupSpec() { FileUtils.deleteQuietly(new File("target/mockVcsDir/")) }

    void "showWithMessage redirects to show"() {
        when:
            final String MSG = "This is a flash message"
            final int ID = 123
            controller.params.id = ID
            controller.params.flashMessage = MSG
            controller.showWithMessage()
        then:
            controller.response.redirectedUrl == "/model/$ID".toString()
            controller.flash.giveMessage == MSG
    }

    void "share throws an exception if no id is specified"() {
        when:
            controller.share()
        then:
            def err = thrown(Exception)
            err.getClass() == java.lang.Exception
            err.message == "Model version must be specified to share"
    }

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
        File clone = new File("target/mockVcsDir/wd/")
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
        File exchangeDir = new File("target/mockVcsDir/ed/")
        exchangeDir.mkdirs()
        grailsApplication.config.jummp.vcs.exchangeDirectory = exchangeDir.path
        modelService.vcsService.vcsManager = gitService.getInstance()
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
                        "jummp-plugins/jummp-plugin-pharmml/test/files/example2.xml", auth)
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
