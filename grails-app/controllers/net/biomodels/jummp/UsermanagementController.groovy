package net.biomodels.jummp

import grails.converters.JSON
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
**/

import grails.plugin.springsecurity.annotation.Secured
import net.biomodels.jummp.core.user.UserNotFoundException
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.webapp.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.mail.MailAuthenticationException

import javax.mail.AuthenticationFailedException
/**
 * @short Controller for managing user registrations
 *
 * @author <a href="mailto:raza.ali@ebi.ac.uk">Raza Ali</a>
 * @author <a href="mailto:tung.nguyen@ebi.ac.uk">Tung Nguyen</a>
 * @author <a href="mailto:mihai.glont@ebi.ac.uk">Mihai Glont</a>
 */
class UsermanagementController extends CommonController {
    private static final Logger LOGGER = LoggerFactory.getLogger(UsermanagementController.class)
    def simpleCaptchaService
    def userService
    def springSecurityService
    def messageSource
    def notificationService

    private String checkForMessage() {
        String flashMessage = ""
        if (flash.message) {
        	flashMessage = flash.message
        }
        return flashMessage
    }

    private Object checkForErrorBean() {
    	if (flash.validationError) {
    		return flash.validationError
    	}
    	return null
    }

     /**
     * Passes on any info messages needed to be displayed and renders the register gsp
     */
    @Secured(["isAnonymous()"])
    def create() {
        render view: "register", model: [postUrl: "", flashMessage: checkForMessage(), title: "Register | BioModels",
    									validationErrorOn: checkForErrorBean()]
    }

    @Secured(["isAuthenticated()"])
    def edit() {
        if (forwardIfReadOnly()) return
        User currentUser = springSecurityService.currentUser
        List notifications = notificationService.getNotificationPermissions(currentUser.username)
        render  view: "edit",
                model: [serverURL: serverURL, postUrl: "", flashMessage: checkForMessage(),
                        validationErrorOn: checkForErrorBean(),
                        user: currentUser,
                        notificationPermissions: notifications]
    }

    @Secured(["isAuthenticated()"])
    def editPassword() {
        if (forwardIfReadOnly()) return
        String username = springSecurityService.principal.username as String
        render  view: "editPassword",
                model: [postUrl: "", flashMessage: checkForMessage(), title: "Edit Password | BioModels",
                        validationErrorOn: checkForErrorBean(),
                        user: userService.getUser(username)]
    }

    @Secured(["isAuthenticated()"])
    def show() {
        User currentUser = userService.getCurrentUser()
        String username = currentUser.username
        List notifications = notificationService.getNotificationPermissions(username)
        String titlePage = "${userService.getRealName(username)} | BioModels"
        render  view: "show",
                model: [postUrl: "",
                        flashMessage: checkForMessage(),
                        validationErrorOn: checkForErrorBean(),
                        user: currentUser,
                        titlePage: titlePage,
                        notificationPermissions: notifications]
    }

    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def forgot() {
        if (forwardIfReadOnly()) return
        render  view: "forgot",
                model: [postUrl: "", flashMessage: checkForMessage(), title: "Forgot Password | BioModels",
                        validationErrorOn: checkForErrorBean()]
    }

    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def resetPassword() {
        if (params.code) {
            flash.hashCode = params.code.decodeHTML()
            flash.username = params.username.decodeHTML()
            redirect action: 'reset'
        } else {
            redirect action: 'forgot'
        }
    }

    /**
     * Password reset based on the unique code sent to the user
     */
    @Secured(["isAnonymous()"])
    def reset() {
        Map model = [postUrl: "", flashMessage:checkForMessage(), validationErrorOn: checkForErrorBean(),
                     hashCode: flash.hashCode, username: flash.username]
    	render(view: "reset", model: model)
    }

    @Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
    def notfound() {
        String previousURL = request.getHeader("referer")
        // if the callee is not the auth controller, throw an error 405
        if (!previousURL) {
            forward(controller: "errors", action: "error405")
            return
        }
    	render(view: "notfound")
    }


    boolean validateUserData(def cmd, def params) {
        params.username = params.username.decodeHTML()
        params.email = params.email.decodeHTML()
        params.userRealName = params.userRealName.decodeHTML()
        params.institution = params.institution.decodeHTML()
        params.orcid = params.orcid.decodeHTML()
        bindData(cmd, params)
        if (!cmd.validate()) {
            cmd.errors?.allErrors?.each {
                LOGGER.error(messageSource.getMessage(it, Locale.ENGLISH))
            }
            flash.validationError = cmd
            return false
        }
        return true
    }

    /**
     * This action is invoked when changing the password or typing the password on the login form.
     * It is used to verify a compromised password and then show a warning against the user.
     * @return
     */
    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def verifyCompromisedPassword() {
        String password = request.getJSON()["password"].decodeHTML()
        boolean result = false
        if (password) {
            result = userService.isCompromisedPassword(password)
        }
        render([result: result] as JSON)
    }

    /**
     * <p>This action is invoked after logging in successfully if the current password is compromised.
     * Currently, hacking plain password from an authenticated context is not trivial. We insert this check
     * when the login form is submitted via an AJAX call. If the password is compromised, a waring message will
     * be sent to the user email and notification.
     *
     * <p>This action is also called when changing the password if it has been leaked on one or more data breaches.
     * @return
     */
    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def verifyCompromisedPasswordAndNotifyUser() {
        String username = request.getJSON()["username"].decodeHTML()
        String password = request.getJSON()["password"].decodeHTML()
        boolean result = doVerifyCompromisedPassword(username, password)
        render([compromised: result] as JSON)
    }

    private boolean doVerifyCompromisedPassword(final String username, final String password) {
        boolean result = false
        if (password) {
            result = userService.isCompromisedPassword(password)
        }
        if (result && doesUserExist(username)) {
            // email and notify the user
            doEmailAndNotifyDueToCompromisedPassword(username)
        }
        return result
    }
    /**
     * Validates the command object and then uses the user service to
     * edit a user. If an error occurs at any point, the method redirects
     * to edit action and sends the user a helpful message.
     */
    @Secured(["IS_AUTHENTICATED_FULLY"])
    def editUser(EditUserCommand cmd) {
        cmd = cmd.sanitise()
        if (!cmd.validate()) {
            return redirect(action: "edit")
        }
        try {
            def user = cmd.toUser()
        	User user1 = userService.editUser(user)
            def preferences = userService.getPreferences(user1)
        	notificationService.updatePreferences(preferences)
        } catch (Exception e) {
            flash.message = e.getMessage()
            LOGGER.error(e.message, e)
            return redirect(action: "edit")
        }
        flash.message = "Your profile was updated successfully!"
        redirect(action: "show")
    }

    @Secured(["IS_AUTHENTICATED_FULLY"])
    def update(EditUserCommand cmd) {
        if (!cmd.validate()) {
            flash.message = "Your provided data are invalid.";
            render(["message": flash.message, "status": "NOT_OK"] as JSON)
        }
        // 1. Save user's info
        EditUserCommand cmd1 = cmd.sanitise()
        def user = cmd1.toUser()
        def user1 = userService.editUser(user)

        // 2. Save preferences
        String options = cmd.options
        List preferences = userService.getPreferences(user1, options)
        boolean success = notificationService.updatePreferences(preferences)
        String status, message
        if (success && user1.id >= 0) {
            status = "OK"
            message = "Your profile has been updated successfully!"
        } else {
            status = "NOT_OK"
            message = "Your profile cannot be updated due to unknown errors!"
        }
        render([status: status, message: message] as JSON)
    }
    /**
     * Validates the command object and then uses the user service to
     * edit a user. If an error occurs at any point, the method redirects
     * to edit action and sends the user a helpful message.
     */
    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def newPassword(ResetPasswordCommand cmd) {
        cmd = cmd.sanitise()
        withForm {
            if (!cmd.validate()) {
                flash.hashCode = params.hashCode
                redirect(action: "reset")
            }
            try {
                userService.resetPassword(cmd.hashCode, cmd.username, cmd.newPassword)
            } catch (Exception e) {
                flash.message = e.getMessage()
                redirect(action: "reset")
                return
            }
            flash.flashMessage = """The password for ${cmd.username} was updated successfully. Please log in BioModels\
 with your newly updated password."""
            doEmailAndNotifyWhenChangingPassword(cmd.username)
            doVerifyCompromisedPassword(cmd.username, cmd.newPassword)
            redirect(controller: "login", action: "auth")
        }.invalidToken {
            render(controller: "errors", action: "error405")
        }
    }

    /**
     * Validates the command object and then uses the user service to
     * edit a user. If an error occurs at any point, the method redirects
     * to edit action and sends the user a helpful message.
     */
    @Secured(["IS_AUTHENTICATED_FULLY"])
    def updatePassword(UpdatePasswordCommand cmd) {
        cmd = cmd.sanitise()
        withForm {
            if (!cmd.validate()) {
                redirect(action: "editPassword")
                return
            }
            try {
                userService.changePassword(cmd.oldPassword, cmd.newPassword)
            } catch (Exception e) {
                flash.message = e.getMessage()
                redirect(action: "editPassword")
                return
            }
            flash.message = "Your password was updated successfully!"
            String username = springSecurityService.currentUser.username
            doEmailAndNotifyWhenChangingPassword(username)
            doVerifyCompromisedPassword(username, cmd.newPassword)
            redirect(action: "show")
        }.invalidToken {
            render(controller: "errors", action: "error405")
        }
    }

    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def profile() {
        String username = ""
        if (params.containsKey("username")) {
            username = params.get("username").decodeHTML()
        } else if (!springSecurityService.isLoggedIn()) {
            redirect(uri: "/login/auth")
        }
        User currentUser = springSecurityService.currentUser
        if (!username) {
            username = currentUser?.username
        }
        String msg = ""
        if (!springSecurityService.isLoggedIn() || username != currentUser?.username) {
            msg = "You are viewing the public profile of $username"
        } else {
            msg = "You are viewing your full profile"
        }
        render(view: "profile", model: [msg: msg])
    }

    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def registration() {
        if (forwardIfReadOnly()) return
        User currentUser = springSecurityService.currentUser
        if (currentUser) {
            redirect(action: "profile")
        } else {
            forward(action: "create")
        }
    }

    /**
     * Requests a password link from the user service, hiding the exception thrown
     * if the username or email address provided does not exist.
     */
    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def requestPassword() {
        withForm {
            String username = params.username.decodeHTML()
            boolean succeeded = true
            boolean usernameExists = true
            String message = ""
            if (username) {
                try {
                    userService.requestPassword(username)
                } catch (UserNotFoundException e) {
                    LOGGER.error(e.message, e)
                    succeeded = false
                    usernameExists = false
                } catch (MailAuthenticationException | AuthenticationFailedException e) {
                    LOGGER.error(e.message, e)
                    succeeded = false
                }
                if (succeeded) {
                    if (username.indexOf("@")) {
                        message = "Thank you. Please check the mailbox of ${username}."
                    } else {
                        message = "Thank you. Please check the email associated with ${username}'s account"
                    }
                } else {
                    if (!usernameExists) {
                        if (username.indexOf("@")) {
                            message = "Email address ${username} does not exist."
                        } else {
                            message = "Username ${username} does not exist."
                        }
                    } else {
                        message = """Cannot send a reset password link to your email. Please contact  us asap for \
further instructions"""
                    }
                }
            } else {
                message = "Please provide a username or an valid email address."
            }
            LOGGER.debug(message)
            flash.message = message
            println(message)
            redirect(action: "forgot")
        }.invalidToken {
            render(controller: "errors", action: "error405")
        }
    }

    /**
     * Performs validation on the captcha, ensures that the empty security parameter is not
     * filled (as is commonly done by robots), validates the command object and then uses
     * the user service to create a user. If an error occurs at any point, the method redirects
     * to create action and sends the user a helpful message.
     */
    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def signUp() {
        withForm {
            RegistrationCommand cmd = new RegistrationCommand()
            if (!validateUserData(cmd, params)) {
                return redirect(action: "registration")
            }
            String captcha = params.captcha
            boolean captchaValid = simpleCaptchaService.validateCaptcha(captcha)
            if (!captchaValid) {
                flash.message = "The text entered did not match the image. Please try again"
                return redirect(action: "registration")
            }
            if (params.verysecure) {
                flash.message = "I hope you are a robot. Otherwise something has gone wrong."
                return redirect(action: "registration")
            }
            Long result = -1
            try {
                result = userService.register(cmd.toUser())
            } catch (Exception e) {
                flash.message = e.getMessage()
                LOGGER.error e.message, e
                return redirect(action: "registration")
            } finally {
                if (result == -1) {
                    LOGGER.error("""An error has happened when trying to create a new account for this user info \
[${cmd.username}, ${cmd.email}, ${cmd.userRealName}, ${cmd?.orcid}]""")
                } else if (result >= 0) {
                    LOGGER.debug("A new account has been created successfully with the user info \
[${result}: ${cmd.username}, ${cmd.email}]")
                }
            }
            render(view: "successfulregistration", model: [email: cmd.email])
        }.invalidToken {
            render(controller: "errors", view: "error405")
        }
    }

    /**
     * Fetch users' data based what customers are typing. The data populate the source of
     * Autocomplete widgets. The data can be customised but they have to include two mandatory
     * fields as label and value. The two fields are formed from the other ones. For example:
     * label = userRealName (username<email>)
     */
    @Secured(["IS_AUTHENTICATED_FULLY"])
    def fetchUsers() {
        String request = params.request
        if (Integer.parseInt(request) == RequestType.SEARCH_TERMS.value) {
            String searchTerm = params.search
            def usersMap = queryUsers(searchTerm)
            render(usersMap as JSON)
        } else { // request == RequestType.SELECT_VALUE
            String username = params.username
            User user = userService.getUser(username)
            render([user] as JSON)
        }
    }

    /**
     * Search for users that are used for adding contributors
     * @return
     */
    @Secured(["IS_AUTHENTICATED_FULLY"])
    def searchUsersForAddContributors() {
        String searchTerm = params.searchTerm?.decodeHTML()
        List users = queryUsers(searchTerm)
        users.each {
            String label = "${it.userRealname} (${it.username}, ${it.email})"
            it.put("label", label)
        }
        Map<String, Object> usersMap = [users: users]
        if (users?.size()) {
            String str = g.render(template: "/contributor/listOfFoundUsers", plugin: "jummp-plugin-web-application",
                model: [users: users, searchTerm: searchTerm]).toString()
            usersMap.put("htmlBasedStringOfUsers", str)
        }
        render(usersMap as JSON)
    }

    /**
     * This controller tries to query the database to get the user who is potentially associated
     * with the fields provided by new users. It is called in the register view where we parse
     * the input values and come up with a Ajax call to UserService in order to look up them
     * into the database.
     *
     * @return JSON string  the query if an user matches with, or an empty string in otherwise.
     */
    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def lookupUser() {
        String query = params?.query?.decodeHTML()
        String col = params?.column
        int column = Integer.parseInt(col)
        User user = userService.lookupUser(query, column)
        String response = ""
        if (user) {
            response = query
        }
        render([response] as JSON)
    }

    private List queryUsers(final String searchTerm) {
        if (!searchTerm) { return [] }
        List users = userService.searchUsers(searchTerm)
        def userList = []
        users.each { def user ->
            def email = user[0]
            def username = user[1]
            def userRealName = user[2]
            def id = user[3]
            userList << [label: "${userRealName} (${username}<${email}>)",
                         value: id,
                         username: username,
                         email: email,
                         userRealname: userRealName]
        }
        userList
    }

    private void doEmailAndNotifyWhenChangingPassword(final String username) {
        final User USER = User.findByUsername(username)
        if (USER) {
            final String link = createLink(controller: "usermanagement", action: "editPassword", absolute: true)
            final String SUBJECT = "[BioModels] Your Password Has Been Updated Successfully"
            final String BODY = """Dear ${USER.person.userRealName},\
<p>We want to inform you that your password has been successfully changed.</p>\
<p>If you did not request this change, please contact us asap.</p>\
<p>Thank you for your cooperation.</p>\
<p>Kind regards,<br/>\
The BioModels Team</p>"""
            userService.sendEmail(USER, BODY, SUBJECT)
        }
    }

    private void doEmailAndNotifyDueToCompromisedPassword(final String username) {
        final User USER = User.findByUsername(username)
        if (USER) {
            final String link = createLink(controller: "usermanagement", action: "editPassword", absolute: true)
            final String SUBJECT = "[BioModels] Compromised Password Alert!"
            final String BODY = """Dear ${USER.person.userRealName},\
<p>Your password has appeared in one or more data breaches which puts your account at high risk of compromise. \
You should change your password immediately.</p>\
<p>Open this link ${link} to change your password.</p>\
<p>Thank you for your cooperation.</p>\
<p>Best regards,<br/>\
The BioModels Team</p>"""
            userService.sendEmail(USER, BODY, SUBJECT)
        }
    }

    private static doesUserExist(final String username) {
        User.findByUsername(username)
    }
}
