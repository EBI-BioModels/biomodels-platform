/**
* Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
* A PARTICULAR PURPOSE. See the GNU Affero General Publicc
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

import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.webapp.Notification
import net.biomodels.jummp.webapp.NotificationType
import net.biomodels.jummp.webapp.NotificationTypePreferences
import net.biomodels.jummp.webapp.NotificationUser
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.core.model.ModelTransportCommand
import org.springframework.security.access.prepost.PreAuthorize

/**
 * Service asynchronously called by Camel plugin, in response to various messages.
 * Sends notifications to users.
 *
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 * @date 20141015
 */
class NotificationService {
    def modelDelegateService
    def grailsApplication
    def mailService
    def springSecurityService
    def messageSource
    def searchService

    Set<User> getUsersFromUsernames(def usernames) {
        return usernames.collect { User.findByUsername(it) }
    }

    Set<User> getNotificationRecipients(ModelTransportCommand model, NotificationType type) {
        Set<String> creators  = model.creatorUsernames
        return getUsersFromUsernames(creators)
    }

    Set<User> getNotificationRecipients(def permissionsMap) {
        def usernames = permissionsMap.findAll { ptc -> ptc.write }
        return getUsersFromUsernames(usernames.collect { it.id })
    }

    NotificationTypePreferences getPreference(User user, NotificationType type) {
        NotificationTypePreferences pref = NotificationTypePreferences.findByUserAndNotificationType(user, type)
        if (!pref) {
            pref = NotificationTypePreferences.getDefault(user, type)
        }
        return pref
    }

    void updatePreferences(List<NotificationTypePreferences> preferences) {
        User user = User.findByUsername(preferences.first().user.username)
        preferences.each { updated ->
            NotificationTypePreferences existing = getPreference(user, updated.notificationType)
            if (existing.sendMail != updated.sendMail || existing.sendNotification != updated.sendNotification) {
                existing.sendMail = updated.sendMail
                existing.sendNotification = updated.sendNotification
                if (!existing.save()) {
                    log.error "Failed to update notification preferences ${existing} for user ${user}"
                }
            }
        }
    }

    void sendNotificationToUser(User user, Notification notification) {
        NotificationTypePreferences pref = getPreference(user, notification.notificationType)
        if (pref.sendMail) {
            String emailBody = notification.body
                String emailSubject = notification.title
                mailService.sendMail {
                    to user.email
                    from grailsApplication.config.jummp.security.registration.email.sender
                    subject emailSubject
                    body emailBody
                }
        }
        if (pref.sendNotification) {
            NotificationUser userNotify = new NotificationUser(notification: notification,
                    user: user)
            if (!userNotify.save()) {
                log.error "Was not able to deliver notification ${userNotify.inspect()}"
            }
        }
    }

    void sendNotification(ModelTransportCommand model, Notification notification, Set<User> watchers) {
        if (!notification.save()) {
            log.error("Notification $notification for users $watchers was not persisted")
        } else {
            watchers.each { sendNotificationToUser(it, notification) }
        }
    }

    void modelPublished(def body) {
        RevisionTransportCommand rev  = body.revision as RevisionTransportCommand
        useGenericNotificationStructure("notification.model.published.title",
                [rev.name] as String[],
                "notification.model.published.body",
                [rev.name, body.user] as String[],
                NotificationType.PUBLISH,
                body.user,
                getNotificationRecipients(body.perms),
                rev.model)
            searchService.makePublic(rev)
    }

    void readAccessGranted(def body) {
        ModelTransportCommand model  = body.model as ModelTransportCommand
        useGenericNotificationStructure("notification.model.readgranted.title",
                [model.name] as String[],
                "notification.model.readgranted.body",
                [model.name, body.user, body.grantedTo.username] as String[],
                NotificationType.ACCESS_GRANTED,
                body.user,
                getNotificationRecipients(body.perms),
                model)

            useGenericNotificationStructure("notification.model.readgrantedTo.title",
                    [model.name] as String[],
                    "notification.model.readgrantedTo.body",
                    [model.name, body.user] as String[],
                    NotificationType.ACCESS_GRANTED_TO,
                    body.user,
                    getUsersFromUsernames([body.grantedTo.username]),
                    model)
    }

    void writeAccessGranted(def body) {
        ModelTransportCommand model  = body.model as ModelTransportCommand
        useGenericNotificationStructure("notification.model.writegranted.title",
                [model.name] as String[],
                "notification.model.writegranted.body",
                [model.name, body.user, body.grantedTo.username] as String[],
                NotificationType.ACCESS_GRANTED,
                body.user,
                getNotificationRecipients(body.perms) - User.findByUsername(body.grantedTo.username),
                model)

            useGenericNotificationStructure("notification.model.writegrantedTo.title",
                    [model.name] as String[],
                    "notification.model.writegrantedTo.body",
                    [model.name, body.user] as String[],
                    NotificationType.ACCESS_GRANTED_TO,
                    body.user,
                    getUsersFromUsernames([body.grantedTo.username]),
                    model)
    }

    void useGenericNotificationStructure(String notificationTitle,
            String[] titleParams, String notificationBody, String[] bodyParams,
            NotificationType type, String sender, Set<User> watchers,
            ModelTransportCommand model) {
        Notification notification = new Notification()
        notification.title = messageSource.getMessage(notificationTitle, titleParams, null)
        notification.body = messageSource.getMessage(notificationBody, bodyParams, null)
        notification.notificationType = type
        notification.sender = User.findByUsername(sender)
        watchers = watchers - [notification.sender]
        sendNotification(model, notification, watchers)
    }

    int unreadNotificationCount() {
        User notificationsFor = User.findByUsername(springSecurityService.authentication.name)
        def notifications = NotificationUser.findAllNotNotificationSeenByUser(notificationsFor)
        if (notifications) {
            return notifications.size()
        }
        return 0
    }

    @PreAuthorize("isAuthenticated()")
    def getNotificationPermissions(String username) {
        User notificationsFor = User.findByUsername(username)
        def retval = []
        NotificationType.values().each {
            retval.add(getPreference(notificationsFor, it))
        }
        return retval
    }

    @PreAuthorize("isAuthenticated()")
    def list(String username, int maxSize = -1) {
        User notificationsFor = User.findByUsername(username)
        def notifications = NotificationUser.findAllByUser(notificationsFor).reverse()
        if (maxSize == -1 || notifications.size() < maxSize) {
            return notifications
        }
        return notifications[0..maxSize-1]
    }

    void markAsRead(def msgID, String username) {
        Notification notification = Notification.get(msgID)
        User notificationsFor = User.findByUsername(username)
        NotificationUser notificationUser = NotificationUser.findByNotificationAndUser(notification, notificationsFor)
        notificationUser.setNotificationSeen(true)
        notificationUser.save()
    }

    void delete(def body) {
        ModelTransportCommand model  = body.model as ModelTransportCommand
        useGenericNotificationStructure("notification.model.deleted.title",
                [model.name] as String[],
                "notification.model.published.body",
                [model.name, body.user] as String[],
                NotificationType.DELETED,
                body.user,
                getNotificationRecipients(body.perms),
                model)
        searchService.setDeleted(model)
    }

    void update(def body) {
        ModelTransportCommand model  = body.model as ModelTransportCommand
        def updates = []
        body.update.each { updates.add(it) }
        useGenericNotificationStructure("notification.model.updated.title",
                [model.name] as String[],
                "notification.model.updated.body",
                [model.name, body.user, updates.join(",")] as String[],
                NotificationType.VERSION_CREATED,
                body.user,
                getNotificationRecipients(body.perms),
                model)
    }
}
