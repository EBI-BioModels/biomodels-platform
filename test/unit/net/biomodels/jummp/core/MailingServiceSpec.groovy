package net.biomodels.jummp.core

import grails.test.mixin.TestFor
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Covers JBM-776: MailingService.send() is the one place all outgoing mail goes through, and the test environment
 * switches it off (jummp.security.mailer.enabled = false in Config.groovy) so that no test sends mail through the
 * Brevo/smtp2go/SMTP account a developer has configured.
 *
 * The unit-test configuration includes the developer's own ~/.jummp.properties, which may hold real Brevo and smtp2go
 * API keys. They are blanked in setup(), so that a message which does get through is handed to the (stubbed)
 * mailService instead of being sent through that account.
 */
@TestFor(MailingService)
class MailingServiceSpec extends Specification {
    private List<Closure> dispatched

    void setup() {
        config.jummp.security.mailer.brevoApiKey = ""
        config.jummp.security.mailer.smtp2goApiKey = ""
        dispatched = []
        service.mailService = [sendMail: { Closure mail -> dispatched << mail }]
    }

    private void send() {
        service.send([to: "someone@example.com", subject: "A subject", text: "A body"])
    }

    void "mail is dispatched when jummp.security.mailer.enabled is not set"() {
        given: "no setting at all, unlike the test environment, whose Config.groovy switches mailing off"
        config.jummp.security.mailer.remove("enabled")

        when:
        send()

        then:
        service.mailingEnabled
        dispatched.size() == 1
    }

    void "mail is not dispatched when jummp.security.mailer.enabled is false"() {
        given:
        config.jummp.security.mailer.enabled = false

        when:
        send()

        then:
        !service.mailingEnabled
        dispatched.isEmpty()
    }

    @Unroll
    void "jummp.security.mailer.enabled = #configured means mailing enabled is #expected"() {
        given:
        config.jummp.security.mailer.enabled = configured

        expect:
        service.mailingEnabled == expected

        where:
        configured | expected
        true       | true
        "true"     | true
        false      | false
        "false"    | false
        "FALSE"    | false
    }

    void "mail is dispatched again once the setting is switched back on"() {
        given:
        config.jummp.security.mailer.enabled = false
        send()

        when:
        config.jummp.security.mailer.enabled = true
        send()

        then:
        dispatched.size() == 1
    }
}
