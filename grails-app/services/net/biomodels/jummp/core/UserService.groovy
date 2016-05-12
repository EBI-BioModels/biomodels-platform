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

import net.biomodels.jummp.core.events.LoggingEventType
import net.biomodels.jummp.core.events.PostLogging
import net.biomodels.jummp.core.user.*
import net.biomodels.jummp.model.PublicationPerson
import net.biomodels.jummp.plugins.security.Person
import net.biomodels.jummp.plugins.security.Role
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.plugins.security.UserRole
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.perf4j.aop.Profiled
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.transaction.TransactionStatus

/**
 * @short Service for User administration.
 *
 * This service is meant for any kind of user management, such as changing password
 * and administrative tasks like enabling/disabling users, etc.
 *
 * @author Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 */
class UserService implements IUserService {
    /**
     * Dependency injection of springSecurityService
     */
    def springSecurityService
    /**
     * Dependency injection of mail Service provided by the Mail plugin
     */
    def mailService
    /**
     * Dependency injection of grails Application
     */
    @SuppressWarnings("GrailsStatelessService")
    def grailsApplication

    def grailsLinkGenerator
    /**
     * Random number generator for creating user validation ids.
     */
    private final Random random = new Random(System.currentTimeMillis())

    private void checkUserValid(String user) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication()
        if (user != auth.getName() && !SpringSecurityUtils.ifAnyGranted("ROLE_ADMIN")) {
            throw new AccessDeniedException("User not valid. You do not have rights to modify this user")
        }
    }

    String getRealName(String userName) {
        User user = User.findByUsername(userName)
        return user.person.userRealName
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

    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag = "userService.changePassword")
    void changePassword(String oldPassword, String newPassword) throws BadCredentialsException {
        User user = (User)springSecurityService.getCurrentUser()
        if (user.password != springSecurityService.encodePassword(oldPassword, null)) {
            throw new BadCredentialsException("Cannot change password, old password is incorrect")
        }
        // TODO: verify password strength?
        user.password = springSecurityService.encodePassword(newPassword, null)
        user.passwordExpired = false
        user.save()
        springSecurityService.reauthenticate(user.username, newPassword)
    }

    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag = "userService.editUser")
    @PreAuthorize("hasRole('ROLE_ADMIN') or isAuthenticated()") //used to be: authentication.name==#username
    void editUser(User user) throws UserInvalidException {
        checkUserValid(user.username)
        User origUser = User.findByUsername(user.username)
        if (origUser.person.orcid != user.person.orcid) {
            Person sameOrcid = Person.findByOrcid(user.person.orcid)
            if (sameOrcid) {
                if (User.findByPerson(sameOrcid)) {
                    log.warn("User ${user.username} tried to register orcid ${user.person.orcid} which is already in use by ${sameOrcid.userRealName}")
                    throw new UserInvalidException("Someone with this ORCID is already registered in the repository", origUser.id)
                }
                else {
                    Person possiblyNoLongerNeeded = origUser.person
                    origUser.person = sameOrcid
                    def anyPublications = PublicationPerson.findByPerson(possiblyNoLongerNeeded)
                    if (!anyPublications) {
                        possiblyNoLongerNeeded.delete()
                    }
                }
            }
            else {
                def anyPublications = PublicationPerson.findByPerson(origUser.person)
                if (!anyPublications) {
                    origUser.person.orcid = user.person.orcid
                }
                else {
                    origUser.person = new Person(orcid: user.person.orcid)
                }
            }
        }
        origUser.person.userRealName = user.person.userRealName
        origUser.person.institution = user.person.institution
        origUser.email = user.email
        if (!origUser.person.validate()) {
        	throw new UserInvalidException(user.username)
        }
        if (!origUser.validate()) {
            throw new UserInvalidException(user.username)
        }
        origUser.person.save(flush: true, failOnError: true)
        origUser.save(flush: true)
    }

    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="userService.getCurrentUser")
    @PreAuthorize("hasRole('ROLE_USER')")
    User getCurrentUser() {
        return User.findByUsername(springSecurityService.authentication.principal.username).sanitizedUser()
    }

    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="userService.getUser")
    @PreAuthorize("hasRole('ROLE_ADMIN') or isAuthenticated()") //used to be: authentication.name==#username
    User getUser(String username) throws UserNotFoundException {
        //checkUserValid(username)  -> dont need to be admin to get a user by their username anymore, legitimate use case -> model sharing
        User user = User.findByUsername(username)
        if (!user) {
            throw new UserNotFoundException(username)
        }
        return user.sanitizedUser()
    }

    @Profiled(tag="userService.hasRole")
    @PreAuthorize("isAuthenticated()")
    boolean hasRole(User user, Role role) {
        UserRole.get(user.id, role.id)
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
    List<String> searchUsers(String term) {
        return User.withCriteria {
            projections {
                property('email')
                property('username')
                person {
                    property('userRealName')
                }
                property('id')
            }
            or {
                ilike 'email', "%"+term + '%'
                ilike 'username', "%"+term + '%'
                person {
                    ilike 'userRealName', "%"+term + '%'
                }
            }
        }
    }

    @PostLogging(LoggingEventType.RETRIEVAL)
    @Profiled(tag="userService.getAllUsers")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    List<User> getAllUsers(Integer offset, Integer count) {
        return User.list([offset: offset, max: Math.min(count, 100)])
    }

    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="userService.enableUser")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    Boolean enableUser(Long userId, Boolean enable) throws UserNotFoundException {
        User user = User.get(userId)
        if (!user) {
            throw new UserNotFoundException(userId)
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

    def generator = { String alphabet, int n ->
        new Random().with {
            (1..n).collect { alphabet[ nextInt( alphabet.length() ) ] }.join()
        }
    }

    @PostLogging(LoggingEventType.CREATION)
    @Profiled(tag = "userService.register")
    @PreAuthorize("isAnonymous() or hasRole('ROLE_ADMIN')")
    Long register(User user) throws RegistrationException, UserInvalidException {
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
                    log.error("cannot save user ${newUser.properties} - ${newUser.errors.allErrors.inspect()}. oops")
                } else {
                    log.debug(newUser)
                }
            }
        }
        else {
            newUser.person.save(flush:true, failOnError:true)
        }
        boolean adminRegistration = false
        String p = generator( (('A'..'Z')+('0'..'9')).join(), 6 )
        if (SpringSecurityUtils.ifAnyGranted("ROLE_ADMIN")) {
            // admin creates with a random password that is emailed to the user.
            newUser.password = "*"
            newUser.enabled = true
            newUser.password = springSecurityService.encodePassword(p, null)
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
            throw new UserInvalidException(user.username)
        }
        String registrationCode = String.valueOf(random.nextInt()) + user.username
        newUser.registrationCode = registrationCode.encodeAsMD5()
        GregorianCalendar registrationInvalidation = new GregorianCalendar()
        registrationInvalidation.add(GregorianCalendar.DAY_OF_MONTH, 1)
        newUser.registrationInvalidation = registrationInvalidation.getTime()
        newUser.save(flush: true)
        UserRole.create(newUser, Role.findByAuthority("ROLE_USER"), true)
        if (grailsApplication.config.jummp.security.curatorByDefault) {
        	UserRole.create(newUser, Role.findByAuthority("ROLE_CURATOR"), true)
        }
        // send out notification mail
        if (grailsApplication.config.jummp.security.registration.email.send) {
            String recipient = newUser.email
            if (grailsApplication.config.jummp.security.registration.email.sendToAdmin) {
                recipient = grailsApplication.config.jummp.security.registration.email.adminAddress
            }
            String emailBody = grailsApplication.config.jummp.security.registration.email.body
            String emailSubject = grailsApplication.config.jummp.security.registration.email.subject
            emailBody = emailBody.replace("{{USERNAME}}", newUser.username)
            emailBody = emailBody.replace("{{PASSWORD}}", p)
            emailBody = emailBody.replace("{{REALNAME}}", newUser.person.userRealName)
            mailService.sendMail {
                to recipient
                from grailsApplication.config.jummp.security.registration.email.sender
                subject emailSubject
                body emailBody
            }
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
    @Profiled(tag="userService.requestPassword")
    @PreAuthorize("isAnonymous()")
    void requestPassword(String username) throws UserNotFoundException {
        User user = User.findByUsername(username)
        if (!user) {
            throw new UserNotFoundException(username)
        }
        String passwordCode = String.valueOf(random.nextInt()) + user.username
        user.passwordForgottenCode = passwordCode.encodeAsMD5()
        GregorianCalendar codeInvalidation = new GregorianCalendar()
        codeInvalidation.add(GregorianCalendar.DAY_OF_MONTH, 1)
        user.passwordForgottenInvalidation = codeInvalidation.getTime()
        user.save(flush: true)
        // send out notification mail
        String recipient = user.email
        String url = grailsLinkGenerator.link(controller: 'usermanagement', action: 'passwordreset', id: user.passwordForgottenCode, absolute: true)
        String emailBody = grailsApplication.config.jummp.security.resetPassword.email.body
        emailBody = emailBody.replace("{{REALNAME}}", user.person.userRealName)
        emailBody = emailBody.replace("{{URL}}", url)
        mailService.sendMail {
                to recipient
                from grailsApplication.config.jummp.security.registration.email.sender
                subject grailsApplication.config.jummp.security.resetPassword.email.subject
                body emailBody
        }
    }

    @PostLogging(LoggingEventType.UPDATE)
    @Profiled(tag="userService.resetPassword")
    @PreAuthorize("isAnonymous()")
    void resetPassword(String code, String username, String password) throws UserNotFoundException, UserCodeInvalidException, UserCodeExpiredException {
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
        user.save(flush: true)
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
                log.error("The initial user could not be created in the database. Is the database configured properly?")
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
}
