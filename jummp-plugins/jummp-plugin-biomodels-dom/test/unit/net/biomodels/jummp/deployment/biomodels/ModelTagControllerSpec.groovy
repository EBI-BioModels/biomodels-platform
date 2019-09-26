package net.biomodels.jummp.deployment.biomodels

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.ModelTag
import net.biomodels.jummp.model.Tag
import net.biomodels.jummp.plugins.security.Person
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.utils.ModelTagBootStrap
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(ModelTagController)
@Mock([Person, User, Model, Tag, ModelTag])
class ModelTagControllerSpec extends Specification {

    Person person
    User user
    Model model
    Tag tag

    def setup() {
        /**
         * Create a model and some tags
         */
        ModelTagBootStrap.initialiseData()
        controller.springSecurityService = [
            encodePassword: 'changeme',
            reauthenticate: { String u -> true},
            loggedIn: true]
    }

    void "test updateModelTag action"() {
        given:
        params.updatedTags = ["Annotated,Fun Model,Sample"]
        params.modelId = "M001"
        def loggedInUser = User.findByUsername("elvis")
        controller.springSecurityService.put("currentUser", loggedInUser)
        def modelTagService = mockFor(ModelTagService)
        modelTagService.demand.update { Set tags, String modelId, User u ->
            Map result = [:]
            result["status"] = 200
            result["message"] = "The model has no longer been tagged any label"
            result
        }
        controller.modelTagService = modelTagService.createMock()
        when: "hit updateModelTag action"
        controller.updateModelTag()
        then: "expect that the action can save data"
        response.status == 200
        response.json.message.contains("The model has no longer been tagged any label")
    }
}
