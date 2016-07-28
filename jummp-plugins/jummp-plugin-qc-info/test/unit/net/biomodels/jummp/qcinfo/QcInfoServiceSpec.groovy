package net.biomodels.jummp.qcinfo

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(QcInfoService)
// need these two to be able to run the last test
@TestMixin(HibernateTestMixin)
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.hibernate.HibernateTestMixin
import spock.lang.Specification


class QcInfoServiceSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "test something"() {
    }
}
