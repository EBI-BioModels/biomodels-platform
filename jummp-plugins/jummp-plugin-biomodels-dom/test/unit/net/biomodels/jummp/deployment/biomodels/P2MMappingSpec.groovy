package net.biomodels.jummp.deployment.biomodels

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestFor(P2MMapping)
class P2MMappingSpec extends Specification {
    String representative
    String member

    def setup() {
        representative = "BMID000000112901"
        member = "BMID000000112902"
    }

    void "test the number of records at initial stage"() {
        expect: "the table has nothing"
        0 == P2MMapping.count
    }

    void "test saving data into the table"() {
        when: "saving a mapping into the database"
        def map = new P2MMapping(representative: representative, member: member)
        map.save()

        then: "the count should be positive"
        P2MMapping.count
    }
}
