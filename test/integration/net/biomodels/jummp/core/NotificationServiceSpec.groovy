package net.biomodels.jummp.core

import grails.test.spock.IntegrationSpec
import net.biomodels.jummp.core.model.ModelTransportCommand as MTC
import net.biomodels.jummp.core.model.RevisionTransportCommand as RTC
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.ModelFormat
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.webapp.NotificationType as NT
import net.biomodels.jummp.webapp.NotificationUser as NU

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
    def sessionFactory

    def setup() {
        jummpIntegrationTest = new JummpIntegrationTest()
        jummpIntegrationTest.createUserAndRoles()
        jummpIntegrationTest.authenticateAnonymous()
        // createUserAndRoles() saves its Role/UserRole rows without flushing; a dynamic
        // finder issued straight after (as getNotificationRecipients() does) can otherwise
        // miss whichever of them wasn't already flushed by something else beforehand.
        sessionFactory.currentSession.flush()
    }

    void "test whether the feedback is notified to the admin group or not"() {
        given: "a body including a star, email and comment"
        expect:
            notificationService.feedback2Admin([star: 4, email: "tung@test.com", comment: "Love it!"])
    }

    void "notifyOmicsdiExportPending emails and notifies the admin group without error"() {
        expect:
            notificationService.notifyOmicsdiExportPending("2026-08-30T22:00:00+0000")
    }

    void "notifyOmicsdiExportPending tolerates a null activity timestamp"() {
        expect:
            notificationService.notifyOmicsdiExportPending(null)
    }

    // Regression coverage for the recipient-resolution gap: curators and admins are granted
    // access to a model via a role-based ACE (see ModelService's model-creation code), never
    // a per-user one, so the principal-only ACL walk in ModelService.getPermissionsMap() can
    // never surface them - getNotificationRecipients() used to paper over only the admin half
    // of that, and only for the single literal username "administrator".
    void "getNotificationRecipients resolves curators and admins by role, not just a hardcoded username"() {
        given: "no per-model ACL entries at all"
        when:
            Set<User> recipients = notificationService.getNotificationRecipients([])
        then: "the role-based curator and admin accounts are included anyway"
            recipients*.username.containsAll(["admin", "curator"])
    }

    // Regression coverage for the revision-owner gap: a revision's submitter is granted
    // permissions on the revision's own ACL (ModelService.doUpdatePermissions), never the
    // model's, so getNotificationRecipients(body.perms) - which only reads the model's ACL -
    // used to miss them entirely whenever they held no separate model-level access.
    void "revisionDeleted notifies the revision's own owner even without model-level ACL access"() {
        given: "a model/revision owned by testuser, who holds no model-level ACL permission"
            Model model = new Model(vcsIdentifier: "test-notif/", submissionId: "NOTIFTEST1")
            Revision revision = new Revision(model: model, vcsId: "abc123", revisionNumber: 1,
                owner: User.findByUsername("testuser"), minorRevision: false, name: "",
                description: "", comment: "", uploadDate: new Date(),
                format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
            model.addToRevisions(revision)
            model.save(flush: true, failOnError: true)

            MTC modelTC = new MTC(id: model.id, name: "Test model", submissionId: model.submissionId)
            RTC revisionTC = new RTC(id: revision.id, revisionNumber: revision.revisionNumber)
            def body = [model: modelTC, revision: revisionTC,
                       user: User.findByUsername("admin"), perms: []]
        when:
            notificationService.revisionDeleted(body)
        then: "the owner (testuser) received an in-app notification despite the ACL gap"
            NU.createCriteria().get {
                eq("user", User.findByUsername("testuser"))
                notification {
                    eq("notificationType", NT.REVISION_DELETED)
                }
            } != null
    }
}
