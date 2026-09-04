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
* A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
* details.
*
* You should have received a copy of the GNU Affero General Public License along
* with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
*
* Additional permission under GNU Affero GPL version 3 section 7
*
* If you modify Jummp, or any covered work, by linking or combining it with
* Spring Framework, Perf4j, Spring Security (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0 the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Spring Framework, Perf4j, Spring Security used as well as
* that of the covered work.}
**/

package net.biomodels.jummp.core

import grails.plugin.cache.Cacheable
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.acl.AclSid
import grails.plugin.springsecurity.userdetails.GrailsUser
import grails.util.Metadata
import groovy.json.JsonSlurper
import net.biomodels.jummp.core.events.LoggingEventType
import net.biomodels.jummp.core.events.PostLogging
import net.biomodels.jummp.core.user.*
import net.biomodels.jummp.plugins.security.Person
import net.biomodels.jummp.plugins.security.Role
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.plugins.security.UserRole
import net.biomodels.jummp.utils.MathUtils
import net.biomodels.jummp.webapp.NotificationType as NT
import net.biomodels.jummp.webapp.NotificationTypePreferences as NTPs
import org.json.JSONObject
import org.perf4j.aop.Profiled
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.mail.MailAuthenticationException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.transaction.TransactionStatus

import javax.mail.AuthenticationFailedException
import java.nio.file.Path
import java.nio.file.Paths

/**
 * @short Service for User administration.
 *
 * This service is meant for any kind of user management, such as changing password
 * and administrative tasks like enabling/disabling users, etc.
 *
 * @author Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
class UserService implements IUserService, InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class)
    def springSecurityService
    def mailingService
    @SuppressWarnings("GrailsStatelessService")
    def grailsApplication
    def grailsLinkGenerator
    /**
     * Random number generator for creating user validation ids.
     */
    private final Random random = new Random(System.currentTimeMillis())

    private static void checkUserValid(String user) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication()
        if (user != auth.getName() && !SpringSecurityUtils.ifAnyGranted("ROLE_ADMIN")) {
            throw new AccessDeniedException("User not valid. You do not have rights to modify this user")
        }
    }

    String getRealName(String userName) {
        User user = User.findByUsername(userName)
        return user.person.userRealName
    }

    String getUsername() {
        String username = "anonymous"
        def principal = springSecurityService.principal
        if (principal instanceof String) {
            username = principal
        } else if (principal instanceof GrailsUser ||
            principal instanceof org.springframework.security.core.userdetails.User) {
            username = principal.username
        }
        return username
    }

    String getUsername(String realName) {
        def usernames = User.withCriteria {
            projections {
                property('username')
            }
            person {
                ilike 'userRealName', realName
             }
        }
        if (usernames) {
            return usernames.get(0)
        }
        return null
    }

    Integer getTotalUserCount() {
        User.count()
    }

    List<User> getUsersByRole(String role) {
        def users =
            UserRole.findAllByRole(Role.findByAuthority(role)).collect {
                it.user }
        return users
    }

    String getEmailAddress() {
        def principal = springSecurityService.getCurrentUser()
        if (principal) {
            return principal.email
        }
        return null
    }

    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag = "userService.changePassword")
    void changePassword(String oldPassword, String newPassword) throws BadCredentialsException {
        oldPassword = oldPassword.decodeHTML()
        newPassword = newPassword.decodeHTML()
        User user = (User)springSecurityService.getCurrentUser()
        if (user.password != springSecurityService.encodePassword(oldPassword, null)) {
            String msg = "Cannot change password, old password is incorrect"
            LOGGER.error("${user.username}: ${msg}")
            throw new BadCredentialsException(msg)
        }
        String hardLevel = MathUtils.checkPasswordStrength(newPassword)
        if (hardLevel != MathUtils.PWD_HARD_LEVEL.X_HARD.label) {
            String msg = """Password is insufficiently strong! A strong password is: at least 12 characters long but \
14 or more is better. It is recommended to be any combination of lowercase and uppercase letters, numbers, and \
symbols (ASCII-standard characters only). Accents and accented characters aren't supported."""
            LOGGER.warn("${user.username}: ${msg}")
            throw new BadCredentialsException(msg)
        }
        user.password = springSecurityService.encodePassword(newPassword, null)
        user.passwordExpired = false
        if (!user.save(flush: true)) {
            String msg = """${user.username} has changed the password but it cannot be saved! \
The cause is ${user.errors.toString()}."""
            LOGGER.error(msg)
        } else {
            springSecurityService.reauthenticate(user.username, newPassword)
            if (isCompromisedPassword(newPassword)) {
                println "Emailed and notified the user"
            }
        }
    }

    void handleOrcidModification(User newUserData, User existing) {
        UpdateOrcid updateOrcid = new UpdateOrcid()
        String oldOrcid = existing.person.orcid
        String newOrcid = newUserData.person.orcid
        UpdateOrcidStrategy updateStrategy
        if (newOrcid) {         /* add/modify ORCID */
            updateStrategy = new AddOrModifyOrcidStrategy()
        } else if (oldOrcid) {  /* remove ORCID */
            updateStrategy =  new RemoveOrcidUserEdit()
        }
        if (updateStrategy) {
            updateOrcid.setStrategy(updateStrategy)
            updateOrcid.oldUser = existing
            updateOrcid.newUser = newUserData
            updateOrcid.update()
        }
    }

    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag = "userService.editUser")
    @PreAuthorize("hasRole('ROLE_ADMIN') or isAuthenticated()") //used to be: authentication.name==#username
    User editUser(User user) throws UserInvalidException {
        checkUserValid(user.username.decodeHTML() as String)
        User origUser = User.findByUsername(user.username.decodeHTML() as String)
        handleOrcidModification(user, origUser)
        origUser.person.userRealName = user.person.userRealName
        origUser.person.institution = user.person.institution
        origUser.email = user.email
        if (!origUser.save(flush: true)) {
            throw new UserUpdateException("""\
Cannot persist the user data ${origUser.id} into the database due to ${origUser.errors.allErrors.inspect()}""", origUser.id)
        }
        origUser
    }

    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="userService.getCurrentUser")
    @PreAuthorize("hasRole('ROLE_USER')")
    User getCurrentUser() {
        final String username = springSecurityService.authentication.principal.username as String
        User u = User.findByUsername(username)?.sanitizedUser()
        LOGGER.debug("Retrieving the current user: ${u?.username}")
        return u
    }

    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="userService.getUser")
    @PreAuthorize("hasRole('ROLE_ADMIN') or isAuthenticated()") //used to be: authentication.name==#username
    User getUser(String username) throws UserNotFoundException {
        //checkUserValid(username)  -> don't need to be admin to get a user by their username anymore, legitimate use case -> model sharing
        User user = User.findByUsername(username)
        if (!user) {
            throw new UserNotFoundException(username)
        }
        return user.sanitizedUser()
    }

    /**
     * This method is being served for the registration process. It is used for looking up the
     * being typed values by a potential user whether they exist/are being used by someone else
     * into our database or not.
     *
     * @param query The username, email or ORCID identifier which is looked up against the database
     * @return A user if it matches the query, or null in the otherwise case
     */
    @PreAuthorize("isAnonymous() or isAuthenticated()")
    User lookupUser(String query, int column) {
        User user
        if (column == 1 || column == 2) {
            user = User.findByUsername(query)
            if (!user) {
                user = User.findByEmail(query)
            }
        } else {
            // column == 3 --> search Person by ORCID identifier
            Person person = Person.findByOrcid(query)
            user = User.findByPerson(person)
        }
        return user
    }

    @Profiled(tag="userService.hasRole")
    @PreAuthorize("isAuthenticated()")
    boolean hasRole(User user, Role role) {
        UserRole.get(user.id, role.id)
    }

    boolean isLoggedInUserACurator() {
        def userId = springSecurityService.getCurrentUserId()
        if (!userId) return false
        isCurator(User.load(userId))
    }

    boolean isLoggedInUserAAdmin() {
        def userId = springSecurityService.getCurrentUserId()
        if (!userId) return false
        isAdmin(User.load(userId))
    }

    @Profiled(tag="userService.isCurator")
    @PreAuthorize("isAuthenticated()")
    boolean isCurator(User u) throws RoleNotFoundException {
        Role curator = Role.findByAuthority('ROLE_CURATOR')
        if (!curator) {
            throw new RoleNotFoundException("Authority ROLE_CURATOR is not defined.")
        }
        hasRole(u, curator)
    }

    @Profiled(tag="userService.isAdmin")
    @PreAuthorize("isAuthenticated()")
    boolean isAdmin(User u) throws RoleNotFoundException {
        Role admin = Role.findByAuthority('ROLE_ADMIN')
        if (!admin) {
            throw new RoleNotFoundException("Authority ROLE_ADMIN is not defined.")
        }
        hasRole(u, admin)
    }

    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="userService.getUser")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    User getUser(Long id) throws UserNotFoundException {
        User user = User.get(id)
        if (!user) {
            throw new UserNotFoundException(id)
        }
        return user
    }

    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="userService.searchUsers")
    @PreAuthorize("hasRole('ROLE_ADMIN') or isAuthenticated()") //used to be: authentication.name==#username
    List searchUsers(String term) {
        List result = User.withCriteria {
            projections {
                property('email')
                property('username')
                person {
                    property('userRealName')
                }
                property('id')
            }
            or {
                ilike 'email', "%${term}%"
                ilike 'username', "%${term}%"
                person {
                    ilike 'userRealName', "%${term}%"
                }
            }
        } as List

        result
    }

    void sendEmail(final User USER, final String BODY, final String SUBJECT) {
        if (USER) {
            sendEmail(USER.email, BODY, SUBJECT)
        }
    }

    void sendEmail(final String toEmail, final String BODY, final String SUBJECT) {
        mailingService.send([to: toEmail, subject: SUBJECT, html: BODY])
    }

    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="userService.getAllUsers")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    List<User> getAllUsers(Integer offset, Integer count) {
        return User.list([offset: offset, max: Math.min(count, 100)])
    }

    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="userService.getAllUsersByRole")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    List<User> getAllUsersByRole(Role role, Integer offset = 0, Integer count = 10) {
        String query = """SELECT ur.user FROM UserRole as ur WHERE ur.role.id = ${role.id}"""
        Map namedParams = [:]
        Map metaParams = [offset: Math.min(count, 100), max: count]
        List<User> users = User.executeQuery(query, namedParams, metaParams)
        return users
    }

    List<User> getAllUsersByRole(String authority, Integer offset = 0, Integer count = 10) {
        if (!authority) { return null }
        Role role = Role.findByAuthority(authority)
        if (!role) { return null }
        getAllUsersByRole(role)
    }

    Integer countUsersByRole(Role role) {
        String query = """SELECT ur.user FROM UserRole as ur WHERE ur.role.id = ${role.id}"""
        List<User> users = User.executeQuery(query)
        users?.size()
    }

    Integer countUsersByRole(String authority) {
        if (!authority) { return 0 }
        Role role = Role.findByAuthority(authority)
        if (!role) { return 0 }
        countUsersByRole(role)
    }

    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="userService.enableUser")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    Boolean enableUser(Long userId, Boolean enable) throws UserNotFoundException {
        User user = User.get(userId)
        if (!user) {
            throw new UserNotFoundException("User with userId = ${userId} doesn't exist!")
        }
        if (user.enabled != enable) {
            user.enabled = enable
            user.save(flush: true)
            return (User.get(userId).enabled == enable)
        } else {
            return false
        }
    }

    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="userService.lockAccount")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    Boolean lockAccount(Long userId, Boolean lock) throws UserNotFoundException {
        User user = User.get(userId)
        if (!user) {
            throw new UserNotFoundException(userId)
        }
        if (user.accountLocked != lock) {
            user.accountLocked = lock
            user.save(flush: true)
            return (User.get(userId).accountLocked == lock)
        } else {
            return false
        }
    }

    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="userService.expireAccount")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    Boolean expireAccount(Long userId, Boolean expire) throws UserNotFoundException {
        User user = User.get(userId)
        if (!user) {
            throw new UserNotFoundException(userId)
        }
        if (user.accountExpired != expire) {
            user.accountExpired = expire
            user.save(flush: true)
            return (User.get(userId).accountExpired == expire)
        } else {
            return false
        }
    }

    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="userService.expirePassword")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    Boolean expirePassword(Long userId, Boolean expire) throws UserNotFoundException {
        User user = User.get(userId)
        if (!user) {
            throw new UserNotFoundException(userId)
        }
        if (user.passwordExpired != expire) {
            user.passwordExpired = expire
            user.save(flush: true)
            return (User.get(userId).passwordExpired == expire)
        } else {
            return false
        }
    }

    @PostLogging(LoggingEventType.CREATION)
    @Profiled(tag = "userService.register")
    @PreAuthorize("isAnonymous() or hasRole('ROLE_ADMIN')")
    Long register(User user, boolean specifiedPassword=false) throws RegistrationException, UserInvalidException {
        if (springSecurityService.authentication instanceof AnonymousAuthenticationToken &&
                !grailsApplication.config.jummp.security.anonymousRegistration) {
            throw new AccessDeniedException("Registration disabled for anonymous users")
        }
        if (User.findByUsername(user.username)) {
            throw new RegistrationException("User with same name already exists", user.username)
        }
        User newUser = user.sanitizedUser()

        if (newUser.person.orcid) {
            def existing = Person.findByOrcid(newUser.person.orcid)
            if (existing) {
                if (User.findByPerson(existing)) {
                    throw new RegistrationException(
                        "Someone with this ORCID is already registered in the repository",
                        newUser.person.orcid)
                } else {
                    existing.userRealName = newUser.person.userRealName
                    existing.institution = newUser.person.institution
                    newUser.person = existing
                }
            } else {
                if (!newUser.person.save(flush: true)) {
                    LOGGER.error("Cannot save user ${newUser.properties} - ${newUser.errors.allErrors.inspect()}. oops")
                } else {
                    LOGGER.debug("User (id: ${newUser.id}, username: ${newUser.username}) has been created!")
                }
            }
        } else {
            newUser.person.save(flush:true, failOnError: true)
        }
        boolean adminRegistration
        final String sequence = (('A'..'Z')+('a..z')+('0'..'9')+"!\"#\$%&'()*+,-./:;<=>?@[\\]^_`{|}~").join()
        String p = MathUtils.generatePassword(sequence, 10)
        if (SpringSecurityUtils.ifAnyGranted("ROLE_ADMIN")) {
            // admin creates with a random password that is emailed to the user.
            newUser.enabled = true
            if (!specifiedPassword) {
                newUser.password = p
            }
            else {
                String passwordSupplied = user.password
                newUser.password = passwordSupplied
            }
            newUser.password = springSecurityService.encodePassword(newUser.password, null)
            newUser.passwordExpired = false
            adminRegistration = true
        } else {
            if (grailsApplication.config.jummp.security.ldap.enabled) {
                // disable password for ldap
                newUser.password = "*"
            } else {
                // TODO: validate the password length?
                newUser.password = springSecurityService.encodePassword(p, null)
            }
            // user (no longer) disabled after registration
            newUser.enabled = true
            newUser.passwordExpired = false
        }

        newUser.accountLocked = false
        newUser.accountExpired = false
        newUser.id = null
        if (!newUser.validate()) {
            newUser.errors?.allErrors?.each {
                LOGGER.error(it.toString())
            }
            throw new UserInvalidException(user.username)
        }
        String registrationCode = String.valueOf(random.nextInt()) + user.username
        newUser.registrationCode = registrationCode.encodeAsMD5()
        GregorianCalendar registrationInvalidation = new GregorianCalendar()
        registrationInvalidation.add(GregorianCalendar.DAY_OF_MONTH, 1)
        newUser.registrationInvalidation = registrationInvalidation.getTime()
        newUser.save(flush: true, failOnError:true)
        new AclSid(sid: newUser.username, principal: true).save(flush: true)
        UserRole.create(newUser, Role.findByAuthority("ROLE_USER"), true)
        if (grailsApplication.config.jummp.security.curatorByDefault) {
        	UserRole.create(newUser, Role.findByAuthority("ROLE_CURATOR"), true)
        }
        // send out notification mail
        if (grailsApplication.config.jummp.security.registration.email.send) {
            String recipient = newUser.email
            def bccRecipients = []
            String emailSubject = grailsApplication.config.jummp.security.registration.email.subject
            if (grailsApplication.config.jummp.security.registration.email.sendToAdmin) {
                bccRecipients = grailsApplication.config.jummp.security.registration.email.adminAddress
                emailSubject = "[BioModels - new registration] ${newUser.username} account has been created"
            }
            String webURL = grailsApplication.config.grails.serverURL
            if (!webURL) {
                webURL = "http://localhost:8080/${Metadata.current.'app.name'}"
            }
            String friendlyName = newUser.person.userRealName ?: newUser.username
            String INNER = """
      <p style="margin:0 0 16px;">Dear ${friendlyName.encodeAsHTML()},</p>
      <p style="margin:0 0 16px;">An account to access <a href="${webURL}" style="color:#0F5CB1;">BioModels</a> has been created for you.</p>
      <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 0 16px;font-size:15px;">
        <tr>
          <td style="padding:4px 20px 4px 0;color:#555555;">Username</td>
          <td style="padding:4px 0;font-family:'Courier New',Courier,monospace;font-weight:bold;">${newUser.username.encodeAsHTML()}</td>
        </tr>
        <tr>
          <td style="padding:4px 20px 4px 0;color:#555555;">Password</td>
          <td style="padding:4px 0;font-family:'Courier New',Courier,monospace;font-weight:bold;">${p.encodeAsHTML()}</td>
        </tr>
      </table>
      <p style="margin:0 0 16px;">This password was generated automatically &mdash; you can change it from your account settings after your first login.</p>
      <p style="margin:0 0 16px;">Log in here: <a href="${webURL}/login/auth" style="color:#0F5CB1;">${webURL}/login/auth</a></p>
      <p style="margin:0 0 16px;">Kind regards,<br/><strong>The BioModels Team</strong><br/>
        <a href="${webURL}" style="color:#0F5CB1;">${webURL}</a>
      </p>"""
            String footerNote = """You are receiving this email because an account was created for you on
      <a href="${webURL}" style="color:#0F5CB1;">BioModels</a>, a repository of mathematical models of biological processes.
      This is an automatically generated email &mdash; replies are not monitored."""
            String emailBody = mailingService.wrapHtml(INNER, [footerNote: footerNote, showMaintainer: true])
            String fromRecipient = grailsApplication.config.jummp.security.registration.email.sender as String
            mailingService.send([to: recipient, from: fromRecipient, subject: emailSubject,
                                 html: emailBody, bcc: bccRecipients ?: null])
        }
        return User.findByUsername(user.username).id
    }

    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="userService.validateRegistration")
    @PreAuthorize("isAnonymous()")
    void validateRegistration(String username, String code) throws UserManagementException {
        User user = User.findByUsername(username)
        if (!user) {
            throw new UserNotFoundException(username)
        }
        if (user.enabled) {
            throw new RegistrationException("User already enabled", username)
        }
        if (user.registrationCode != code) {
            throw new UserCodeInvalidException(username, user.id, code)
        }
        if (!user.registrationInvalidation || user.registrationInvalidation.before(new Date())) {
            throw new UserCodeExpiredException(username, user.id)
        }
        user.enabled = true
        user.registrationCode = null
        user.registrationInvalidation = null
        user.save(flush: true)
    }

    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="userService.validateAdminRegistration")
    @PreAuthorize("isAnonymous()")
    void validateAdminRegistration(String username, String code, String password) throws UserManagementException {
        User user = User.findByUsername(username)
        if (!user) {
            throw new UserNotFoundException(username)
        }
        if (user.registrationCode != code) {
            throw new UserCodeInvalidException(username, user.id, code)
        }
        if (!user.registrationInvalidation || user.registrationInvalidation.before(new Date())) {
            throw new UserCodeExpiredException(username, user.id)
        }
        if (!grailsApplication.config.jummp.security.ldap.enabled) {
            if (password) {
                user.password = springSecurityService.encodePassword(password, null)
            } else {
                user.password = "*"
            }
        }
        user.passwordExpired = false
        user.registrationCode = null
        user.registrationInvalidation = null
        user.save(flush: true)
    }

    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="userService.validateAdminRegistration")
    @PreAuthorize("isAnonymous()")
    void validateAdminRegistration(String username, String code) throws UserManagementException {
        this.validateAdminRegistration(username, code, null)
    }

    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag = "userService.unlockAccount")
    @PreAuthorize("isAnonymous()")
    boolean unlockAccount(final String username) {
        User user = User.findByUsername(username)
        if (!user || !user.accountLocked) {
            return false
        } else {
            user.accountLocked = false
            if (!user.save(flush: true)) {
                LOGGER.error("Cannot unlock the user ${username} because of ${user.errors.toString()}.")
                return false
            } else {
                return true
            }
        }
    }

    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="userService.requestPassword")
    @PreAuthorize("isAnonymous()")
    void requestPassword(String usernameOrEmail)
        throws UserNotFoundException, AuthenticationFailedException, MailAuthenticationException  {
        // the second param of the following call is useless
        User user = lookupUser(usernameOrEmail, 1)
        if (!user) {
            throw new UserNotFoundException(usernameOrEmail)
        }
        String passwordCode = String.valueOf(random.nextInt()) + user.username
        user.passwordForgottenCode = passwordCode.encodeAsMD5()
        GregorianCalendar codeInvalidation = new GregorianCalendar()
        codeInvalidation.add(GregorianCalendar.DAY_OF_MONTH, 1)
        user.passwordForgottenInvalidation = codeInvalidation.getTime()
        user.save(flush: true)
        // send out notification mail
        String recipient = user.email
        String url = grailsLinkGenerator.link(controller: 'usermanagement', action: 'resetPassword',
            params: [code: user.passwordForgottenCode, username: user.username], absolute: true)
        String website = grailsApplication.config.grails.serverURL
        String emailBody = grailsApplication.config.jummp.security.resetPassword.email.body
        emailBody = emailBody.replace("{{REALNAME}}", user.person.userRealName)
        emailBody = emailBody.replace("{{USERNAME}}", user.username)
        emailBody = emailBody.replace("{{URL}}", url)
        emailBody = emailBody.replace("{{WEBSITE}}", website)
        mailingService.send([to: recipient,
                             subject: grailsApplication.config.jummp.security.resetPassword.email.subject as String,
                             text: emailBody])
    }

    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="userService.resetPassword")
    @PreAuthorize("isAnonymous()")
    void resetPassword(String code, String username, String password)
        throws UserNotFoundException, UserCodeInvalidException, UserCodeExpiredException {
        User user = User.findByUsername(username)
        if (!user) {
            throw new UserNotFoundException(username)
        }
        if (user.passwordForgottenCode != code) {
            throw new UserCodeInvalidException(username, user.id, code)
        }
        if (!user.passwordForgottenInvalidation || user.passwordForgottenInvalidation.before(new Date())) {
            throw new UserCodeExpiredException(username, user.id)
        }
        // TODO: in case of LDAP we should not change the password
        user.passwordForgottenCode = null
        user.passwordForgottenInvalidation = null
        user.password = springSecurityService.encodePassword(password, null)
        // reset password expired state
        user.passwordExpired = false
        String msg = ""
        if (user.save(flush: true)) {
            msg = "Update the password successfully"
        } else {
            msg = "Updated the password unsuccessfully"
        }
        LOGGER.debug(msg)
        println msg
    }

    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="userService.getAllRoles")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    List<Role> getAllRoles() {
        return Role.listOrderById()
    }

    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="userService.getRolesForUser")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    List<Role> getRolesForUser(Long id) {
        return Role.executeQuery("SELECT role FROM UserRole AS userRole JOIN userRole.role AS role JOIN userRole.user AS user WHERE user.id=:id ORDER BY role.id", [id: id])
    }

    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="userService.addRoleToUser")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    void addRoleToUser(Long userId, Long roleId) throws UserNotFoundException, RoleNotFoundException {
        User user = User.get(userId)
        if (!user) {
            throw new UserNotFoundException(userId)
        }
        Role role = Role.get(roleId)
        if (!role) {
            throw new RoleNotFoundException(roleId)
        }
        if (!UserRole.get(userId, roleId)) {
            UserRole.create(user, role, true)
        }
    }

    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="userService.removeRoleFromUser")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    void removeRoleFromUser(Long userId, Long roleId) throws UserNotFoundException, RoleNotFoundException {
        User user = User.get(userId)
        if (!user) {
            throw new UserNotFoundException(userId)
        }
        Role role = Role.get(roleId)
        if (!role) {
            throw new RoleNotFoundException(roleId)
        }
        UserRole.remove(user, role, true)
    }

    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="userService.removeRoleFromUser")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    Role getRoleByAuthority(String authority) throws RoleNotFoundException {
        Role role = Role.findByAuthority(authority)
        if (!role) {
            throw new RoleNotFoundException(authority)
        }
        return role
    }


    boolean createAdmin(UserCommand user) {
        User person = new User()
        person.properties = user
        boolean userCreated = false
        if (grailsApplication.config.jummp.security.ldap.enabled) {
            // no password for ldap
            person.password = "*"
        } else {
            person.password = springSecurityService.encodePassword(user.password)
        }
        person.enabled = true
        person.accountExpired = false
        person.accountLocked = false
        person.passwordExpired = false
        if (person.validate()) {
            if (persistAdminWithRoles(person)) {
                userCreated = true
            } else {
                LOGGER.error("The initial user could not be created in the database. Is the database configured properly?")
                userCreated = false
            }
        } else {
            userCreated = false
        }
        return userCreated
    }

    boolean persistAdminWithRoles(User person) {
        boolean ok = true
        User.withTransaction { TransactionStatus status ->
            if (!person.save()) {
                ok = false
                status.setRollbackOnly()
            }
            if (!createRolesForAdmin(person)) {
                ok = false
                status.setRollbackOnly()
            }
        }
        return ok
    }

    boolean createRolesForAdmin(User user) {
        Role adminRole = new Role(authority: "ROLE_ADMIN")
        if (!adminRole.save(flush: true)) {
            return false
        }
        addRoleToUser(user.id, adminRole.id)
        Role userRole = new Role(authority: "ROLE_USER")
        if (!userRole.save(flush: true)) {
            return false
        }
        addRoleToUser(user.id, userRole.id)
        return true
    }

    /**
     * Creates a list of notification references for a specific user
     * @param user {@link User} object
     * @param options {@link String} object indicating the options selected from Web UI/UX
     * @return a {@link List} of {@link NTPs} objects which are saved for the user
     */
    List<NTPs> getPreferences(User user, final String options = null) {
        final int nbNotificationTypes = NT.values().length
        Map<Integer, Object> mapOptions = new HashMap<>()

        if (options) {
            def lstOptions = new JsonSlurper().parseText(options)
            for (option in lstOptions) {
                JSONObject jsonObject = new JSONObject(option)
                mapOptions.put(jsonObject['id'] as int, jsonObject)
                // check the implementation at frontend
                LOGGER.info("${jsonObject['id']}|${jsonObject['slug']}\t\t\t${jsonObject['notify']}|${jsonObject['email']}")
            }
        } else {
            for (int i = 1; i <= nbNotificationTypes; i++) {
                JSONObject object = new JSONObject()
                object.put("notify", 1)
                object.put("email", 1)
                mapOptions.put(i, object)
            }
        }

        List<NTPs> preferences = new LinkedList<NTPs>()
        boolean sendEmail, sendNotification
        for (int i = 1; i <= nbNotificationTypes; i++) {
            NT type = NT.getById(i)
            sendNotification = mapOptions.get(i)['notify'] == 1
            sendEmail = mapOptions.get(i)['email'] == 1
            NTPs pref = new NTPs(user: user, notificationType: type, sendMail: sendEmail, sendNotification: sendNotification)
            preferences.add(pref)
        }
        preferences
    }

    @Cacheable("isCompromisedPassword")
    boolean isCompromisedPassword(final String password) {
        String path = grailsApplication.config.jummp.dirs.upload as String
        Path top100kPath = Paths.get(path, "PwnedPasswordsTop100k.json")
        if (top100kPath) {
            def jsonSlurper = new JsonSlurper()
            def arrBreaches = jsonSlurper.parse(new File(top100kPath.toString()))
            return password in arrBreaches
        }
        true
    }

    @Cacheable("isAllowedMigrationAWS")
    String isAllowedMigrationAWS(final String username) {
        String path = grailsApplication.config.jummp.dirs.upload as String
        Path disallowedUsers = Paths.get(path, "DisallowedUsers.json")
        String redirectURL = ""
        if (disallowedUsers) {
            def jsonSlurper = new JsonSlurper()
            def arrBreaches = jsonSlurper.parse(new File(disallowedUsers.toString()))
            if (username in arrBreaches) {
                redirectURL = grailsApplication.config.grails.serverURL + "/usermanagement/notfound"
            }
        }
        redirectURL
    }

    @Override
    void afterPropertiesSet() throws Exception {
        LOGGER.info("Finished the bean initialisation")
    }

    static interface UpdateOrcidStrategy {
        void updateUser(User oldUser, User newUser)
    }

    static class AddOrModifyOrcidStrategy implements UpdateOrcidStrategy {
        @Override
        void updateUser(User oldUser, User newUser) {
            String orcid4NewUser = newUser.person.orcid
            Person potential = Person.findByOrcid(orcid4NewUser)
            if (potential) {
                User user = User.findByPerson(potential)
                if (user && user != oldUser) {
                    String msg = """\
User ${newUser.username} tried to register orcid ${orcid4NewUser} which is already in use \
by ${potential.userRealName}"""
                    LOGGER.warn(msg)
                    msg = "Someone with this ORCID (${orcid4NewUser}) is already registered in the repository"
                    throw new UserInvalidException(msg, oldUser.id)
                } else {
                    oldUser.person = potential
                }
            } else {
                oldUser.person.orcid = orcid4NewUser
            }
        }
    }

    static class RemoveOrcidUserEdit implements UpdateOrcidStrategy {
        void updateUser(User oldUser, User newUser) {
            oldUser.person.orcid = null
        }
    }

    class UpdateOrcid {
        UpdateOrcidStrategy strategy
        User oldUser
        User newUser

        void update() {
            this.strategy.updateUser(oldUser, newUser)
        }
    }
}
