package net.biomodels.jummp

import grails.plugin.springsecurity.annotation.Secured
import net.biomodels.jummp.model.Model

/**
 * This controller is used for testing some features relevant to the submission flow and sending emails to submitters.
 * It is expensive to submit many models to test the functionality of sending emails.
 */
@Secured("ROLE_ADMIN")
class TestingController {
    def modelDelegateService
    def springSecurityService

    /**
     * Testing sending emails to submitters
     * @return
     */
    def sendEmail2Submitters() {
        def notification = [
            model: modelDelegateService.getModel(Model.get(1).submissionId),
            user: springSecurityService.currentUser,
            emails: [springSecurityService.getCurrentUser().email, springSecurityService.getCurrentUser().email]
        ]
        sendMessage("seda:model.create", notification)
        render "sent a test email to your mailbox"
    }

}
