/**
* Copyright (C) 2010-2022 EMBL-European Bioinformatics Institute (EMBL-EBI),
* Deutsches Krebsforschungszentrum (DKFZ)
*
* This file is part of Jummp.
*
* Jummp is free software; you can redistribute it and/or modify it under the
* terms of the GNU Affero General Public License as published by the Free
* Software Foundation; either version 3 of the License, or (at your option) any
* later version.
*
* Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
* A PARTICULAR PURPOSE. See the GNU Affero General Public.
*
* You should have received a copy of the GNU Affero General Public License along
* with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
*
* Additional permission under GNU Affero GPL version 3 section 7
*
* If you modify Jummp, or any covered work, by linking or combining it with
* Apache Commons, Perf4j (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Apache Commons, Perf4j used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.core

import grails.transaction.Transactional
import net.biomodels.jummp.core.model.ModelFormatTransportCommand as MFTC
import net.biomodels.jummp.core.model.ModelTransportCommand as MTC
import net.biomodels.jummp.core.model.PublicationTransportCommand as PTC
import net.biomodels.jummp.core.model.RevisionTransportCommand as RTC
import net.biomodels.jummp.plugins.security.Role
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.plugins.security.UserRole
import net.biomodels.jummp.webapp.Notification
import net.biomodels.jummp.webapp.NotificationType as NT
import net.biomodels.jummp.webapp.NotificationTypePreferences as NTPs
import net.biomodels.jummp.webapp.NotificationUser as NU
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.i18n.LocaleContextHolder as LCH
import org.springframework.security.access.prepost.PreAuthorize

/**
 * Service asynchronously called by Camel plugin, in response to various messages.
 * Sends notifications to users.
 *
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 * @date 20160330
 */
@Transactional
class NotificationService implements InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class)
    def grailsApplication
    def mailingService
    def springSecurityService
    def messageSource

    String serverURL

    void sendConfirmationOrNotificationEmail(final String emailFrom, final String emailTo,
                                             final String emailSubject, final String emailBody,
                                             final String emailReplyTo = null) {
        mailingService.send([to: emailTo, from: emailFrom, subject: emailSubject,
                             html: wrapInHtmlTemplate(emailBody), replyTo: emailReplyTo ?: null])
    }

    private String wrapInHtmlTemplate(String content) {
        return """
<div style="background-color:#f4f4f4;margin:0;padding:32px 0;font-family:Arial,Helvetica,sans-serif;color:#333333;">
  <div style="max-width:620px;margin:0 auto;background-color:#ffffff;border-radius:4px;overflow:hidden;box-shadow:0 2px 6px rgba(0,0,0,0.10);">
    <div style="background-color:#ED6B21;height:5px;"></div>
    <div style="background-color:#072C55;padding:24px 32px 20px;">
      <div style="font-size:22px;font-weight:bold;color:#ffffff;letter-spacing:0.5px;">BioModels</div>
      <div style="font-size:12px;color:rgba(255,255,255,0.75);margin-top:4px;letter-spacing:0.3px;">Laboratory for Systems Medicine &bull; University of Florida</div>
    </div>
    <div style="padding:32px;font-size:15px;line-height:1.7;color:#333333;">
      ${content}
    </div>
    <hr style="border:none;border-top:1px solid #e8e8e8;margin:0;"/>
    <div style="background-color:#f8f8f8;padding:20px 32px;font-size:12px;color:#777777;line-height:1.6;">
      This is an automatically generated email from
      <a href="${serverURL}" style="color:#0F5CB1;">BioModels</a> &mdash; replies are not monitored.<br/>
      BioModels is maintained by the Laboratory for Systems Medicine,
      Department of Medicine, Division of Pulmonary &ndash; Systems Medicine,
      <a href="https://systemsmedicine.pulmonary.medicine.ufl.edu/biomodels/" style="color:#0F5CB1;">University of Florida</a>.<br/>
      &copy; ${new Date().format("YYYY")} University of Florida Health
    </div>
  </div>
</div>"""
    }

    void useGenericNotificationStructure(String notificationTitle,
                                         String[] titleParams, String notificationBody, String[] bodyParams,
                                         NT type, User sender, Set<User> watchers, MTC model) {
        Notification notification = new Notification()
        notification.title = "[BioModels] ${messageSource.getMessage(notificationTitle, titleParams, null)}"
        notification.body = messageSource.getMessage(notificationBody, bodyParams, null)
        notification.notificationType = type
        notification.sender = sender
        notification.dateCreated = new Date()
        if (watchers.contains(notification.sender)) {
            watchers.remove(notification.sender)
        }
        sendNotification(model, notification, watchers)
    }

    Set<User> getNotificationRecipients(def permissionsMap) {
        def writeAccessList = permissionsMap.findAll { ptc -> ptc.write }
        List<User> recipients = writeAccessList.collect { User.get(it.id as Long) }
        // Administrator should be notified for administration and tracking
        User administrator = User.findByUsername("administrator")
        if (administrator) {
            recipients.add(administrator)
        }
        recipients
    }

    NTPs getPreference(User user, NT type) {
        NTPs pref = NTPs.findByUserAndNotificationType(user, type)
        if (!pref) {
            pref = NTPs.getDefault(user, type)
        }
        return pref
    }

    boolean updatePreferences(List<NTPs> preferences) {
        User user = preferences.first().user
        boolean success = true
        preferences.each { updated ->
            NTPs existing = getPreference(user, updated.notificationType)
            if (existing.sendMail != updated.sendMail || existing.sendNotification != updated.sendNotification) {
                existing.sendMail = updated.sendMail
                existing.sendNotification = updated.sendNotification
                if (!existing.save(flush: true)) {
                    success = false
                    logger.error "Failed to update notification preferences ${existing} for user ${user}"
                }
            }
        }
        success
    }

    /**
     * Initialises all options of receiving push notifications or emails for a specific user
     * @param user a {@link User} indicating the specific user
     * @param preferences a{@link List} of all {@link NTPs} indicating the preferences
     * @return true|false
     */
    boolean initialisePreferences(final User user, final List<NTPs> preferences) {
        boolean success = true
        List<String> errors = []
        for (NTPs ntp in preferences) {
            def r = ntp.save(flush: true)
            if (!r) {
                success = false
                errors.add("""Failed to create a new notification preferences ${ntp} for user ${user?.username}, \
caused by ${ntp?.errors?.toString()}""")
            }
        }
        if (!success) {
            errors.each {
                logger.error(it)
            }
        }
        success
    }

    void sendNotificationToUser(User user, Notification notification) {
        final updatedNotifyBody = notification.body.replace("USER_REALNAME", user.person.userRealName)
        notification.body = updatedNotifyBody
        NTPs pref = getPreference(user, notification.notificationType)
        if (pref.sendMail) {
            final String emailFrom = grailsApplication.config.jummp.security.registration.email.sender
            final String emailTo = user.email
            String emailSubject = notification.title
            String emailBody = notification.body
            try {
                sendConfirmationOrNotificationEmail(emailFrom, emailTo, emailSubject, emailBody)
            } catch (Exception e) {
                logger.warn("Skipped notification email to ${emailTo}: ${e.message}")
            }
        }
        if (pref.sendNotification) {
            NU userNotify = new NU(notification: notification, user: user)
            if (!userNotify.save(flush: true)) {
                logger.error "Was not able to deliver notification ${userNotify.errors.allErrors}"
            }
        }
    }

    void sendNotification(MTC model, Notification notification, Set<User> watchers) {
        if (!notification.save(flush: true)) {
            logger.error("Notification $notification for users $watchers was not persisted due to ${notification.errors.inspect()}")
        } else {
            final String originalNotifyBody = notification.body
            watchers.each {
                sendNotificationToUser(it, notification)
                // After sending a notification to the user {it} (i.e., the dear USER_REALNAME has been replaced accordingly,
                // the body has to be recovered for the next receipt.
                notification.body = originalNotifyBody
            }
        }
    }

    void modelCreated(def body) {
        MTC model = body.model
        String serverURL = grailsApplication.config.grails.serverURL
        String modelLink = "${serverURL}/${model.submissionId}"
        User submitter = body.user
        String submitterRealName = submitter.person.userRealName
        String submitterEmail = submitter.email
        String adminEmail = grailsApplication.config.jummp.security.registration.email.adminAddress
        String emailFrom = grailsApplication.config.jummp.security.registration.email.sender
        String emailSubject
        String emailBody

        /* email notification to the curators' mailing list */
        String curatorMailingList = body.emails[0] as String
        if (!curatorMailingList) {
            curatorMailingList = adminEmail
            logger.debug("Missing the curator mailing list in the configuration file!")
        }
        if (curatorMailingList) {
            emailSubject = messageSource.getMessage("notification.model.created.emailToCurator.subject",
                [model.id.toString(), model.submissionId] as String[], null)
            MFTC formatTC = model.format
            String format = "${formatTC.identifier} (${formatTC.name}) (version: ${formatTC.formatVersion})"
            String submitterInfo = "${submitterRealName} (${submitterEmail})"
            PTC ptc = model.publication
            String pubData = ptc ? ptc.prettierPrint() : "&emsp;not yet published"
            GregorianCalendar cal = new GregorianCalendar()
            String submissionTime = cal.getTime().toGMTString()
            String[] args = [model.id.toString(), model.name, model.submissionId, format,
                             submitterInfo, pubData, submissionTime, modelLink]
            emailBody = messageSource.getMessage("notification.model.created.emailToCurator.body", args, null)
            sendConfirmationOrNotificationEmail(emailFrom, curatorMailingList, emailSubject, emailBody)
        }
        /* email notification to the submitter */
        String emailTo = body.emails[1] ?: submitterEmail
        if (emailTo) {
            emailSubject = messageSource.getMessage("notification.model.created.emailToSubmitter.subject",
                [model.submissionId] as String[], null)
            String salutation = submitterRealName ?: "submitter"
            // retrieve the message codes and populate the arguments to them according to two cases
            // 1. the submission was provided the publication
            // 2. the submission wasn't added the publication
            // So that we can add a custom message to suggest citing BioModels if the manuscript is peer-reviewing.
            String withPubMsgCode = "notification.model.created.emailToSubmitter.body.withPublicationProvided"
            String noPubMsgCode = "notification.model.created.emailToSubmitter.body.noPublicationProvided"
            String withPubProvided = messageSource.getMessage(withPubMsgCode, [] as String[], null)
            String noPubProvided = messageSource.getMessage(noPubMsgCode, [model.submissionId] as String[], null)
            String pubInfo = model.publication ? withPubProvided : noPubProvided
            String[] args = [salutation, model.name, model.submissionId, pubInfo, modelLink, curatorMailingList]
            emailBody = messageSource.getMessage("notification.model.created.emailToSubmitter.body", args, null)

            sendConfirmationOrNotificationEmail(emailFrom, emailTo, emailSubject, emailBody)
        }
    }

    void modelPublished(def body) {
        RTC rev  = body.revision as RTC
        String notifyTitle = "notification.model.published.title"
        String notifyBody = "notification.model.published.body"
        User user = body.user as User
        Set<User> receipts = getNotificationRecipients(body.perms)
        final String modelURL = rev.url()
        useGenericNotificationStructure(notifyTitle, [rev.name] as String[],
            notifyBody, [rev.name, user.username, modelURL, "${serverURL}/user", serverURL] as String[],
            NT.PUBLISH, user, receipts, rev.model)
    }

    void modelReadAccessGranted(def body) {
        MTC model  = body.model as MTC
        String notifyTitle = "notification.model.read.granted.title"
        String notifyBody = "notification.model.read.granted.body"
        User user = body.user as User
        User grantedTo = body.grantedTo
        Set<User> receipts = getNotificationRecipients(body.perms)
        useGenericNotificationStructure(notifyTitle, [model.name] as String[], notifyBody ,
            [model.name, user.username, grantedTo.username, model.url(), "${serverURL}/user", serverURL] as String[],
            NT.ACCESS_GRANTED, user, receipts, model)

        notifyTitle = "notification.model.read.grantedTo.title"
        notifyBody = "notification.model.read.grantedTo.body"
        receipts = [grantedTo]
        useGenericNotificationStructure(notifyTitle, [model.name] as String[], notifyBody,
            [model.name, user.username, model.url(), "${serverURL}/user", serverURL] as String[],
            NT.ACCESS_GRANTED_TO, user, receipts, model)
    }

    void modelWriteAccessGranted(def body) {
        MTC model  = body.model as MTC
        String notifyTitle = "notification.model.write.granted.title"
        String notifyBody = "notification.model.write.granted.body"
        User user = body.user as User
        User grantedTo = body.grantedTo
        Set<User> watchers = getNotificationRecipients(body.perms) - [user]
        useGenericNotificationStructure(notifyTitle, [model.name] as String[], notifyBody,
            [model.name, user.username, grantedTo.username, model.url(), "${serverURL}/user", serverURL] as String[],
            NT.ACCESS_GRANTED, user, watchers , model)

        notifyTitle = "notification.model.write.grantedTo.title"
        notifyBody = "notification.model.write.grantedTo.body"
        watchers = [grantedTo] as Set
        useGenericNotificationStructure(notifyTitle, [model.name] as String[], notifyBody,
            [model.name, user.username, model.url(), "${serverURL}/user", serverURL] as String[],
            NT.ACCESS_GRANTED_TO, user, watchers, model)
    }

    int unreadNotificationCount() {
        User notificationsFor = User.findByUsername(springSecurityService.authentication.name)
        def notifications = NU.findAllNotNotificationSeenByUser(notificationsFor)
        if (notifications) {
            return notifications.size()
        }
        return 0
    }

    @PreAuthorize("isAuthenticated()")
    def getNotificationPermissions(String username) {
        User notificationsFor = User.findByUsername(username)
        def retval = []
        NT.values().each {
            retval.add(getPreference(notificationsFor, it))
        }
        return retval
    }

    @PreAuthorize("isAuthenticated()")
    def list(String username, int maxSize = -1) {
        User notificationsFor = User.findByUsername(username)
        def notifications = NU.findAllByUser(notificationsFor).reverse()
        if (maxSize == -1 || notifications.size() < maxSize) {
            return notifications
        }
        return notifications[0..maxSize-1]
    }

    void markAsRead(def msgID, String username) {
        if (msgID && username) {
            Notification notification = Notification.get(msgID)
            User notificationsFor = User.findByUsername(username)
            NU notificationUser = NU.findByNotificationAndUser(notification, notificationsFor)
            notificationUser.setNotificationSeen(true)
            notificationUser.save(flush: true)
        }
    }

    void modelDelete(def body) {
        MTC model  = body.model as MTC
        String notifyTitle = "notification.model.deleted.title"
        String notifyBody = "notification.model.deleted.body"
        User user = body.user as User
        useGenericNotificationStructure(notifyTitle, [model.name] as String[], notifyBody,
            [model.name, user.username, "${serverURL}/user", serverURL] as String[],
            NT.DELETED, user, getNotificationRecipients(body.perms), model)
    }

    /**
     * Sending a notification to the subscribers when the model is updated.
     * @param body
     */
    void modelUpdate(def body) {
        MTC model  = body.model as MTC
        RTC revision = body.revision as RTC
        def updates = []
        body.update.each { updates.add(it) }
        User user = body.user as User
        String notifyTitle = "notification.model.updated.title"
        String notifyBody = "notification.model.updated.body"
        Set<User> recipients = getNotificationRecipients(body.perms)
        String tmp = recipients.collect { User u ->
            "${u.username} (${u.person.userRealName})"
        }.toString()
        logger.debug("People will receive the notification: ${tmp}")
        useGenericNotificationStructure(notifyTitle, [model.name] as String[],
            notifyBody, [revision.url(), model.name, user.username, updates.join("<br/>"), "${serverURL}/user", serverURL] as String[],
            NT.VERSION_CREATED, user, recipients, model)
    }

    void modelSubmitForPublication(def body) {
        RTC revision = body.revision as RTC
        MTC model = revision.model
        User user = body.user as User
        String notificationTitle = "notification.model.sub4pub.title"
        String subjectPrefix = model.publication ? "Publication Request" : "Pre-publication Request"
        String[] titleParams = [subjectPrefix, model.submissionId, revision.name] as String[]
        String notificationBody = "notification.model.sub4pub.body"
        String serverURL = grailsApplication.config.grails.serverURL
        String modelLink = "${serverURL}/${revision.identifier()}"
        String prePubCode = "notification.model.sub4pub.body.prePublishText"
        String prePublishText = model.publication ? "" : messageSource.getMessage(prePubCode, [] as String[], LCH.getLocale())
        String[] bodyParams = [modelLink, revision?.name, prePublishText, user?.person?.userRealName, user.email] as String[]
        Set<User> watchers = getNotificationRecipients(body.perms)
        // send a notification message to the members of the curator group
        useGenericNotificationStructure(notificationTitle, titleParams,
            notificationBody, bodyParams, NT.SUBMIT_FOR_PUBLICATION, user, watchers, model)
        // send an email to biomodels' cura mailing list
        String curatorsEmail = grailsApplication.config.jummp.model.curators.mailinglist as String
        String senderAddr    = grailsApplication.config.jummp.security.registration.email.sender as String
        String emailSubject  = messageSource.getMessage(notificationTitle, titleParams, LCH.getLocale())
        String emailBody     = messageSource.getMessage(notificationBody, bodyParams, LCH.getLocale())
        sendConfirmationOrNotificationEmail(senderAddr, curatorsEmail, emailSubject, emailBody, user.email)

        // send a citation reminder email to the submitter
        notificationTitle = "biomodels.howtoCiteUs.reminder.title"
        emailSubject = messageSource.getMessage(notificationTitle, [] as String[], LCH.getLocale())
        notificationBody = "biomodels.howtoCiteUs.reminder.content"
        bodyParams = [serverURL, user?.person?.userRealName ?: user.username, model.submissionId]
        emailBody = messageSource.getMessage(notificationBody, bodyParams, LCH.getLocale())
        sendConfirmationOrNotificationEmail(senderAddr, user.email, emailSubject, emailBody, curatorsEmail)
    }

    void feedback2Admin(def body) {
        User user = body.user as User
        if (!user) {
            user = User.findByUsername("anonymous")
        }
        String receiverRolesSetting  = grailsApplication.config.jummp.feedback.receiver.roles
        List<String> rolesSetting = receiverRolesSetting.split(",").collect {
            it.trim()
        }
        List<Role> roles = Role.findAllByAuthorityInList(rolesSetting)
        List<UserRole> userRoles = UserRole.findAllByRoleInList(roles)
        List<User> observers = userRoles.collect {
            it.user
        }
        Set<User> watchers = new HashSet<User>(observers)
        String notifTitle = "notification.jummp.feedback.title"
        String notifBody = "notification.jummp.feedback.body"
        useGenericNotificationStructure(notifTitle, [body.star] as String[], notifBody,
            [body.star, body.email, body.comment] as String[], NT.FEEDBACK_ARRIVED, user, watchers, null)
    }

    @Override
    void afterPropertiesSet() throws Exception {
        serverURL = grailsApplication.config.grails.serverURL
        logger.info("Finished the bean initialisation")
    }
}
