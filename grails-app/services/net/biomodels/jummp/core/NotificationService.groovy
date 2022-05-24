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
class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class)
    def grailsApplication
    def mailService
    def springSecurityService
    def messageSource

    void useGenericNotificationStructure(String notificationTitle,
                                         String[] titleParams, String notificationBody, String[] bodyParams,
                                         NT type, User sender, Set<User> watchers, MTC model) {
        Notification notification = new Notification()
        notification.title = messageSource.getMessage(notificationTitle, titleParams, null)
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
        def recipients = writeAccessList.collect { User.get(it.id) }
        recipients
    }

    NTPs getPreference(User user, NT type) {
        NTPs pref = NTPs.findByUserAndNotificationType(user, type)
        if (!pref) {
            pref = NTPs.getDefault(user, type)
        }
        return pref
    }

    void updatePreferences(List<NTPs> preferences) {
        User user = preferences.first().user
        preferences.each { updated ->
            NTPs existing = getPreference(user, updated.notificationType)
            if (existing.sendMail != updated.sendMail || existing.sendNotification != updated.sendNotification) {
                existing.sendMail = updated.sendMail
                existing.sendNotification = updated.sendNotification
                if (!existing.save(flush: true)) {
                    logger.error "Failed to update notification preferences ${existing} for user ${user}"
                }
            }
        }
    }

    void sendNotificationToUser(User user, Notification notification) {
        NTPs pref = getPreference(user, notification.notificationType)
        if (pref.sendMail) {
            String emailBody = notification.body
            String emailSubject = notification.title
            mailService.sendMail {
                to user.email
                from grailsApplication.config.jummp.security.registration.email.sender
                subject emailSubject
                html emailBody
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
            watchers.each { sendNotificationToUser(it, notification) }
        }
    }

    void modelCreated(def body) {
        MTC model = body.model
        String serverURL = grailsApplication.config.grails.serverURL
        String modelLink = "${serverURL}/${model.submissionId}"
        User submitter = body.user
        String submitterRealName = submitter.person.userRealName
        String submitterEmail = submitter.email
        String emailTo
        String emailFrom = grailsApplication.config.jummp.security.registration.email.sender
        String emailSubject
        String emailBody

        /* email notification to the curators' mailing list */
        emailTo = body.emails[0]
        if (emailTo) {
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
            mailService.sendMail {
                async true
                to emailTo
                from emailFrom
                subject emailSubject
                html emailBody
            }
        }
        /* email notification to the submitter */
        emailTo = body.emails[1]
        if (emailTo) {
            emailSubject = messageSource.getMessage("notification.model.created.emailToSubmitter.subject",
                [model.submissionId] as String[], null)
            String salutation = ""
            if (null != submitterRealName) {
                salutation = submitterRealName
            } else {
                salutation = "submitter"
            }
            String withPubMsgCode = "notification.model.created.emailToSubmitter.body.withPublicationProvided"
            String noPubMsgCode = "notification.model.created.emailToSubmitter.body.noPublicationProvided"
            String withPublicationProvided = messageSource.getMessage(withPubMsgCode, [] as String[],  null)
            String noPublicationProvided = messageSource.getMessage(noPubMsgCode, [model.submissionId] as String[], null)
            // embed the instructions about citing BioModels regardless of publication details
            String askAcknowledgement = noPublicationProvided
            String[] args = [salutation, model.name, model.submissionId, askAcknowledgement, modelLink]
            emailBody = messageSource.getMessage("notification.model.created.emailToSubmitter.body", args, null)
            mailService.sendMail {
                async true
                to emailTo
                from emailFrom
                subject emailSubject
                html emailBody
            }
        }
    }

    void modelPublished(def body) {
        RTC rev  = body.revision as RTC
        String notifTitle = "notification.model.published.title"
        String notifBody = "notification.model.published.body"
        User user = body.user as User
        useGenericNotificationStructure(notifTitle, [rev.name] as String[], notifBody,
            [rev.name, user.username] as String[], NT.PUBLISH, user, getNotificationRecipients(body.perms), rev.model)
    }

    void readAccessGranted(def body) {
        MTC model  = body.model as MTC
        String notifTitle = "notification.model.readgranted.title"
        String notifBody = "notification.model.readgranted.body"
        User user = body.user as User
        User grantedTo = body.grantedTo
        useGenericNotificationStructure(notifTitle, [model.name] as String[], notifBody ,
            [model.name, user.username, grantedTo.username] as String[],
            NT.ACCESS_GRANTED, user, getNotificationRecipients(body.perms), model)

        notifTitle = "notification.model.readgrantedTo.title"
        notifBody = "notification.model.readgrantedTo.body"
        useGenericNotificationStructure(notifTitle, [model.name] as String[], notifBody,
            [model.name, user.username] as String[], NT.ACCESS_GRANTED_TO, user, [grantedTo] as Set, model)
    }

    void writeAccessGranted(def body) {
        MTC model  = body.model as MTC
        String notifTitle = "notification.model.writegranted.title"
        String notifBody = "notification.model.writegranted.body"
        User user = body.user as User
        User grantedTo = body.grantedTo
        useGenericNotificationStructure(notifTitle, [model.name] as String[], notifBody,
            [model.name, user.username, body.grantedTo.username] as String[],
            NT.ACCESS_GRANTED, user,
            getNotificationRecipients(body.perms) - [grantedTo, user], model)

        notifTitle = "notification.model.writegrantedTo.title"
        notifBody = "notification.model.writegrantedTo.body"
        useGenericNotificationStructure(notifTitle, [model.name] as String[], notifBody,
            [model.name, user.username] as String[], NT.ACCESS_GRANTED_TO, user, [grantedTo] as Set, model)
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

    void delete(def body) {
        MTC model  = body.model as MTC
        String notifTitle = "notification.model.deleted.title"
        String notifBody = "notification.model.deleted.body"
        User user = body.user as User
        useGenericNotificationStructure(notifTitle, [model.name] as String[], notifBody,
            [model.name, user.username] as String[], NT.DELETED, user, getNotificationRecipients(body.perms), model)
    }

    /**
     * Sending a notification to the subscribers when the model is updated.
     * @param body
     */
    void update(def body) {
        MTC model  = body.model as MTC
        def updates = []
        body.update.each { updates.add(it) }
        User user = body.user as User
        String notifTitle = "notification.model.updated.title"
        String notifBody = "notification.model.updated.body"
        Set<User> recipients = getNotificationRecipients(body.perms)
        List tmp = recipients.collect { User u ->
            "${u.username} (${u.person.userRealName})"
        }.toString()
        logger.debug("People will receive the notification: ${tmp}")
        useGenericNotificationStructure(notifTitle, [model.name] as String[], notifBody,
            [model.name, user.username, updates.join(", ")] as String[], NT.VERSION_CREATED, user, recipients, model)
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
        String emailTo = grailsApplication.config.jummp.model.curators.mailinglist
        String emailFrom = user.email //grailsApplication.config.jummp.security.registration.email.sender
        String emailSubject = messageSource.getMessage(notificationTitle, titleParams, LCH.getLocale())
        String emailBody = messageSource.getMessage(notificationBody, bodyParams, LCH.getLocale())
        mailService.sendMail {
            async true
            to emailTo
            from emailFrom
            replyTo emailFrom
            subject emailSubject
            html emailBody
        }

        // send an email to the user to request a citation to BioModels
        (emailFrom, emailTo) = [emailTo, emailFrom]
        emailFrom = grailsApplication.config.jummp.model.curators.mailinglist
        notificationTitle = "biomodels.howtoCiteUs.reminder.title"
        titleParams = []
        emailSubject = messageSource.getMessage(notificationTitle, titleParams, LCH.getLocale())

        notificationBody = "biomodels.howtoCiteUs.reminder.content"
        bodyParams = [serverURL, user?.person?.userRealName ?: user.username, model.submissionId]
        emailBody = messageSource.getMessage(notificationBody, bodyParams, LCH.getLocale())
        mailService.sendMail {
            async true
            to emailTo
            from emailFrom
            replyTo emailFrom
            subject emailSubject
            html emailBody
        }
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
}
