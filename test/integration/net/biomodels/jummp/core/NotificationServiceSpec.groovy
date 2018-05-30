package net.biomodels.jummp.core

import grails.test.spock.IntegrationSpec

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */

/**
 * @author  Tung Nguyen <tung.nguyen@ebi.ac.uk>
 * @date    21/09/17.
 */
class NotificationServiceSpec extends IntegrationSpec {
    def jummpIntegrationTest
    def notificationService

    def setup() {
        jummpIntegrationTest = new JummpIntegrationTest()
        jummpIntegrationTest.createUserAndRoles()
        jummpIntegrationTest.authenticateAnonymous()
    }

    void "test whether the feedback is notified to the admin group or not"() {
        given: "a body including a star, email and comment"
        expect:
            notificationService.feedback2Admin([star: 4, email: "tung@test.com", comment: "Love it!"])
    }
}
