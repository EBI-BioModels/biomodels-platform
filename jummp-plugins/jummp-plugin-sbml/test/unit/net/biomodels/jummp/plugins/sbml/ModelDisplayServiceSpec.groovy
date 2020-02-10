package net.biomodels.jummp.plugins.sbml

import com.google.gson.JsonObject
import grails.test.mixin.TestMixin
import grails.test.mixin.services.ServiceUnitTestMixin
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Chinmay Arankalle <carankalle@ebi.ac.uk>
 *
 */
@TestMixin(ServiceUnitTestMixin)
class ModelDisplayServiceSpec extends Specification {
    ModelDisplayService service

    void setup() {
        service = applicationContext.modelDisplayService
    }

    static doWithSpring = {
        modelDisplayService(ModelDisplayService) { bean ->
            grailsApplication = ref 'grailsApplication'
        }
    }

    void "test getComponents"() {

        when: "The service method's getComponents is called"
        grailsApplication.config.jummp.model.modeldisplay.server.access = "http://localhost:8887/api/access"
        def result = service.getComponents("BIOMD0000000001",1,0,10,"species")

        then: "it should contain records"
        result.species.size() > 0

        then: "it should contain correct records"
        List<JsonObject> species = result.species as List<JsonObject>
        species.find() {value->
            (value.get("speciesId") == "BLL")
        }

        when: "Tested with reactions"
        result = service.getComponents("BIOMD0000000001",1,0,10,"reactions")

        then: "it should have atleast one reaction"
        result.reactions.size() > 0


    }
}
