package net.biomodels.jummp.core

import grails.plugin.springsecurity.acl.AclSid
import grails.test.spock.IntegrationSpec
import grails.util.Holders
import net.biomodels.jummp.plugins.security.Person
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.webapp.Notification
import net.biomodels.jummp.webapp.NotificationType

import static org.junit.Assert.assertNotNull

class NotificationSpec extends IntegrationSpec {
    def jummpIntegrationTest
    def springSecurityService = Holders.applicationContext.getBean("springSecurityService")

    def setup() {
        jummpIntegrationTest = new JummpIntegrationTest()
        jummpIntegrationTest.createUserAndRoles()
        jummpIntegrationTest.authenticateAnonymous()
    }

    void "test the notification having a 255 character longer body"() {
        given: "a notification with the body exceeds 255 characters"
            String title = "BioModels feedback"
            String body = """\
Hi all, I was just looking at the new biomodels interface, specifically the repressilator model at
https://wwwdev.ebi.ac.uk/biomodels/BIOMD0000000012#Overview. I could not find any download button
on the page to download all files at once, i.e. download complete_model which contains all the files.
It would be great if there could be a COMBINE archive which contains all the file, and if available
metadata for the respective resources.Also if there is a SED-ML for the curation it would be great
to have this with the files. Best Matthias König"""
            Date dateCreated = new Date()
            NotificationType type = NotificationType.FEEDBACK_ARRIVED
            Person person
            User user
            if (!User.findByUsername("testuser")) {
                person = new Person(userRealName: "Test")
                user = new User(username: "testuser",
                    password: springSecurityService.encodePassword("secret"),
                    person: person,
                    email: "test@test.com",
                    enabled: true,
                    accountExpired: false,
                    accountLocked: false,
                    passwordExpired: false)
                assertNotNull(person.save(flush: true, failOnError: true))
                assertNotNull(user.save(flush: true, failOnError: true))
                assertNotNull(new AclSid(sid: user.username, principal: true).save(flush: true))
            } else {
                user = User.findByUsername("testuser")
            }
            assertNotNull(user)
            Notification notification = new Notification(title: title, body: body,
                dateCreated: dateCreated, notificationType: type, sender: user)
        expect:
            notification.validate()
    }
}
