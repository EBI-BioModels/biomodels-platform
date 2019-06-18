package net.biomodels.jummp.deployment.biomodels

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import net.biomodels.jummp.model.Model
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestFor(P2MMapping)
@Mock(Model)
class P2MMappingSpec extends Specification {
    Model representative
    String member

    def setup() {
        representative = new Model(submissionId: "BMID000000112901")
        representative.save(flush: true)
        member = "BMID000000112902"
    }

    def cleanup() {

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
