package net.biomodels.jummp.deployment.biomodels

import grails.plugin.springsecurity.SpringSecurityService
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import net.biomodels.jummp.model.Tag
import net.biomodels.jummp.plugins.security.Person
import net.biomodels.jummp.plugins.security.User
import spock.lang.Specification

import java.text.SimpleDateFormat

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(TagController)
@Mock([Person, User, Tag])
class TagControllerSpec extends Specification {

    void setup() {
        Person person = new Person(userRealName: 'Elvis Nguyen')
        User user = new User(person: person, username: "elvis", email: 'elvis.nguyen@itersdesktop.com', enabled: true,
            password: 'changeme', accountExpired: false, accountLocked: false, passwordExpired: false)
        user.save(flush: true)
        User.count() == 1
        controller.springSecurityService = [
            encodePassword: 'changeme',
            reauthenticate: { String u -> true},
            loggedIn: true]
    }

    void "test index action"() {
        given: "a mock tagService"
        def tagService = mockFor(TagService)
        def springSecurityService = mockFor(SpringSecurityService)
        User userCreated = User.findByUsername("elvis")
        Date dateCreated = new Date()
        Date dateModified = new Date()
        tagService.demand.getAll { ->
            def tag = new Tag(name: "Annotated", userCreated: userCreated)
            /**
             * There is an unexplainable bug that may come up from Grails as well as Spock testing framework.
             * We cannot instantiate the object having these properties dateCreated and dateModified.
             * Instead of inline initialisation, these properties are populated later when the object has been risen.
             * If we do call new Tag(name: "Annotated", userCreated: userCreated, dateModified: dateModified),
             * the instance's dateCreated property will be null.
             *
             * See the other cases: ModelOfTheMonthControllerSpec
             */
            tag.description = "This is a fully annotated model"
            tag.dateCreated = dateCreated
            tag.dateModified = dateModified
            tag.id = 1
            [TagTransportCommand.fromTag(tag)] as List
        }
        controller.springSecurityService = springSecurityService.createMock()
        controller.tagService = tagService.createMock()
        when: "hit index action"
        def result = controller.index()
        then: "the response should include one tag"
        result.tags.size() == 1
        TagTransportCommand firstTag = result.tags.first()
        firstTag.name == "Annotated"
        firstTag.userCreated == userCreated.username
        String df = "yyyy-MM-dd'T'HH:mm:ss"
        firstTag.dateCreated == dateCreated.format(df)
        firstTag.dateModified == dateModified.format(df)
    }

    void "test show action"() {
        given: "a tag saved into database"
        User userCreated = User.findByUsername("elvis")
        Date dateCreated = new Date()
        Date dateModified = new Date()
        Tag tag = new Tag(name: "Annotated", userCreated: userCreated)
        tag.description = "This is a fully annotated model"
        tag.dateCreated = dateCreated
        tag.dateModified = dateModified
        tag.save()
        assert Tag.count == 1

        when: "call show action"
        params.id = 1
        def result = controller.show()

        then: "receive the value"
        Tag returned = result.tag
        null != returned
        returned.name == "Annotated"
        returned.userCreated.username == "elvis"
        // why do the statements fail?
        /*returned.dateCreated == dateCreated
        returned.dateModified == dateModified*/
        SimpleDateFormat sdf = result.dateFormat
        String dP = "yyyy-MM-dd'T'HH:mm:ss"
        sdf.toPattern() == dP
        sdf.format(returned.dateCreated) == sdf.format(dateCreated)
        sdf.format(returned.dateModified) == sdf.format(dateModified)
    }

    void "test add action"() {
        given: "a mock UserService"
        def loggedInUser = User.findByUsername("elvis")
        /**
         * The following snippet cannot work with Spock testing framework
         * def springSecurityService = mockFor(SpringSecurityService)
         * springSecurityService.demand.getCurrentUser {
            if (loggedInUser instanceof GrailsUser) {
                loggedInUser.id
            } else {
                loggedInUser.username
            }
         }
         controller.springSecurityService = springSecurityService.createMock()
         */

        /* move the following snippet to setup() */
        /*controller.springSecurityService = [
            encodePassword: 'changeme',
            reauthenticate: { String u -> true},
            loggedIn: true,
            currentUser: loggedInUser ]*/
        controller.springSecurityService.put("currentUser", loggedInUser)
        when: "invoke add action"
        def result = controller.add()

        then:
        200 == response.status
        result.tag.userCreated.username == loggedInUser.username
        assert result.tag.dateCreated
        assert result.tag.dateModified
        String dP = "yyyy-MM-dd'T'HH:mm:ss"
        result.dateFormat.toPattern() == dP
    }

    void "test createOrUpdate action without initialising values for params"() {
        when: "call createOrUpdate action"
        controller.createOrUpdate()
        then: "get an invalid command object"
        view == "/tag/save"
        model.title == "Tag saved unsuccessfully"
        model.message.contains("There have been errors while doing a data binding for the object")
    }

    void "test createOrUpdate action with initialising values for params"() {
        given: "a logged in user"
        def loggedInUser = User.findByUsername("elvis")
        controller.springSecurityService.put("currentUser", loggedInUser)
        when: "mock TagService"
        def tagService = mockFor(TagService)
        tagService.demand.createOrUpdate { TagTransportCommand cmd ->
            Tag tag = new Tag(name: cmd.name, userCreated: loggedInUser)
            tag.description = cmd.description
            tag.dateCreated = new Date()
            tag.dateModified = new Date()
            tag.save(flush: true)
        }
        controller.tagService = tagService.createMock()
        and: "initialise actual values for params"
        params.name = "My funny tag"
        params.description = "This is my model"
        params.userCreated = loggedInUser.username
        String dP = "yyyy-MM-dd'T'HH:mm:ss"
        SimpleDateFormat sdf = new SimpleDateFormat(dP)
        Date current = new Date()
        params.dateCreated = sdf.format(current)
        params.dateModified = sdf.format(current)
        and: "hit createOrUpdate action"
        controller.createOrUpdate()
        then:
        model.title == "Tag saved successfully"
        model.message == "Tag '${params.name}' saved successfully"
        Tag.count == 1
    }
}

