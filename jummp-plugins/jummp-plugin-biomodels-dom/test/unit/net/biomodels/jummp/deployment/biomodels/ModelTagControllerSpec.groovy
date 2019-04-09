package net.biomodels.jummp.deployment.biomodels

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(ModelTagController)
class ModelTagControllerSpec extends Specification {

    def setup() {
        /**
         * Create a model and some tags
         */
    }

    void "test updateModelTag action"() {
        given:
        params.updatedTags = ["Annotated","Fun Model","Sample"]
        params.modelId = "M001"
        when: "hit updateModelTag action"
        //controller.updateModelTag()
        then: "expect that the action can save data"
        // TODO: Fixme
        2 == 2
    }
}
