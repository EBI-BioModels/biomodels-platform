package net.biomodels.jummp.deployment.biomodels

import grails.test.mixin.Mock
import grails.test.mixin.TestMixin
import grails.test.mixin.services.ServiceUnitTestMixin
import grails.test.mixin.TestFor
import spock.lang.Specification
import net.biomodels.jummp.model.*
import net.biomodels.jummp.plugins.security.*
/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestMixin(ServiceUnitTestMixin)
@TestFor(CurationNotesService)
@Mock([Person, User, ModelFormat, Revision, Model, CurationNotes])
class CurationNotesServiceSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "test something"() {

    }
}
