package net.biomodels.jummp.plugins.sbml

import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.services.ServiceUnitTestMixin
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(BpToModelDisplayService)
@TestMixin(ServiceUnitTestMixin)

class BpToModelDisplayServiceSpec extends Specification {

    void "test BpToModelDisplayService"() {

        when: "The service method's getComponentsFromBP is called"
        Map result = service.getComponentsFromBP("BIOMD0000000292")

        then: "it should contain records"
        result.species.size() > 0
        result.reactions.size() > 0

        then: "it should contain correct records"

        Map map = result.species
        map.keySet().contains("NADPH")


    }
}
