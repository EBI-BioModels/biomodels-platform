package net.biomodels.jummp.core.locks

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(DistributedLockService)
class DistributedLockServiceSpec extends Specification {

    void "test something"() {
        expect:
        service.properties
    }
}
