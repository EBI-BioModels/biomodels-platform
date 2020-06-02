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
**/


import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import net.biomodels.jummp.core.user.UserNotFoundException
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.utils.InputParameterSanitizer
import net.biomodels.jummp.webapp.EditUserCommand
import net.biomodels.jummp.webapp.RegistrationCommand
import net.biomodels.jummp.webapp.ResetPasswordCommand
import net.biomodels.jummp.webapp.UpdatePasswordCommand
import org.springframework.mail.MailAuthenticationException

import javax.mail.AuthenticationFailedException

/**
 * @short Controller for managing user registrations
 *
 * @author <a href="mailto:raza.ali@ebi.ac.uk">Raza Ali</a>
 * @author <a href="mailto:tung.nguyen@ebi.ac.uk">Tung Nguyen</a>
 * @author <a href="mailto:mihai.glont@ebi.ac.uk">Mihai Glont</a>
 */
class UsermanagementController {
    /**
     * Dependency injection for the springSecurityService.
     */
    //def springSecurityService
    def simpleCaptchaService
    def userService
    def springSecurityService
    def messageSource
    def notificationService

    private String checkForMessage() {
        String flashMessage=""
        if (flash.message) {
        	flashMessage=flash.message
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
        render view: "register", model: [postUrl: "", flashMessage: checkForMessage(),
    									validationErrorOn: checkForErrorBean()]
    }

    @Secured(["isAuthenticated()"])
    def edit() {
        User currentUser = springSecurityService.currentUser
        render  view: "edit",
                model: [postUrl: "", flashMessage: checkForMessage(),
                        validationErrorOn: checkForErrorBean(),
                        user: currentUser,
                        notificationPermissions: notificationService.getNotificationPermissions(currentUser.username)]
    }

    @Secured(["isAuthenticated()"])
    def editPassword() {
        render  view: "editPassword",
                model: [postUrl: "", flashMessage: checkForMessage(),
                        validationErrorOn: checkForErrorBean(),
                        user: userService.getUser(springSecurityService.principal.username)]
    }

    @Secured(["isAuthenticated()"])
    def show() {
        User currentUser = userService.getCurrentUser()
        render  view: "show",
                model: [postUrl: "", flashMessage: checkForMessage(),
                        validationErrorOn: checkForErrorBean(),
                        user: currentUser,
                        notificationPermissions: notificationService.getNotificationPermissions(currentUser.username)]
    }

    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def forgot() {
        render  view: "forgot",
                model: [postUrl: "", flashMessage: checkForMessage(),
                        validationErrorOn: checkForErrorBean()]
    }

    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def resetPassword() {
        if (params.id) {
            flash.hashCode = params.id
            redirect action: 'reset'
        } else {
            redirect action: 'forgot'
        }
    }

    /**
    * Password reset based on the unique code sent to the user
    **/
    @Secured(["isAnonymous()"])
    def reset() {
    	render view: "reset", model: [postUrl: "", flashMessage:checkForMessage(),
    								validationErrorOn: checkForErrorBean(),
    								hashCode: flash.hashCode]
    }


    boolean validateUserData(def cmd, def params) {
        bindData(cmd, params)
        if (!cmd.validate()) {
            cmd.errors?.allErrors?.each{
                log.error(messageSource.getMessage(it, Locale.ENGLISH))
            }
            flash.validationError = cmd
            return false
        }
        return true
    }


    /**
     * Validates the command object and then uses the user service to
     * edit a user. If an error occurs at any point, the method redirects
     * to edit action and sends the user a helpful message.
     */
    @Secured(["IS_AUTHENTICATED_FULLY"])
    def editUser() {
        EditUserCommand cmd = new EditUserCommand()
        if (!validateUserData(cmd, params)) {
            return redirect(action:"edit")
        }
        try {
            def user = cmd.toUser()
        	User user1 = userService.editUser(user)
        	notificationService.updatePreferences(cmd.getPreferences(user1))
        }
        catch(Exception e) {
            flash.message = e.getMessage()
            log.error(e.message, e)
            return redirect(action:"edit")
        }
        flash.message = "Profile was updated successfully"
        redirect(action:"show")
    }

    /**
     * Validates the command object and then uses the user service to
     * edit a user. If an error occurs at any point, the method redirects
     * to edit action and sends the user a helpful message.
     */
    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def newPassword() {
    	ResetPasswordCommand cmd=new ResetPasswordCommand()
    	if (!validateUserData(cmd, params)) {
    		flash.hashCode=params.hashCode
    		return redirect(action:"reset")
    	}
        try
    	{
    		userService.resetPassword(cmd.hashCode, cmd.username, cmd.newPassword)
    	}
    	catch(Exception e) {
    		flash.message="password.reset.service.error"
   			return redirect(action:"reset")
    	}
    	flash.flashMessage="Password for ${cmd.username} was updated successfully. Please try logging in now"
    	redirect(controller: "login", action:"auth")
    }

    /**
     * Validates the command object and then uses the user service to
     * edit a user. If an error occurs at any point, the method redirects
     * to edit action and sends the user a helpful message.
     */
    @Secured(["IS_AUTHENTICATED_FULLY"])
    def updatePassword() {
    	UpdatePasswordCommand cmd=new UpdatePasswordCommand()
    	if (!validateUserData(cmd, params)) {
    		return redirect(action:"editPassword")
    	}
        try
    	{
    		userService.changePassword(cmd.oldPassword, cmd.newPassword)
    	}
    	catch(Exception e) {
    		flash.message=e.getMessage();
   			return redirect(action:"editPassword")
    	}
    	flash.message="Password was updated successfully"
    	redirect(action:"show")
    }

    /**
     * Requests a password link from the user service, hiding the exception thrown
     * if the username provided does not exist.
     */
    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def requestPassword() {
        String username = InputParameterSanitizer.encodeAsHTML(params.username)
        boolean succeeded = true
        boolean usernameExists = true
        String message = ""
        if (username) {
            try {
                userService.requestPassword(username)
            } catch (UserNotFoundException e) {
                log.error(e.message, e)
                succeeded = false
                usernameExists = false
            } catch (MailAuthenticationException | AuthenticationFailedException e) {
                log.error(e.message, e)
                succeeded = false
            }
            if (succeeded) {
                message = "Thank you. Please check the email associated with ${username}'s account"
            } else {
                if (!usernameExists) {
                    message = "Username ${username} does not exist."
                } else {
                    message = "Cannot send a reset password link to your email due to authentication error from the mail server"
                }
            }
        } else {
            message = "Please provide a username."
        }
        log.debug(message)
        flash.message = message
        redirect(action: "forgot")
    }

     /**
     * Performs validation on the captcha, ensures that the empty security parameter is not
     * filled (as is commonly done by robots), validates the command object and then uses
     * the user service to create a user. If an error occurs at any point, the method redirects
     * to create action and sends the user a helpful message.
     */
    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
    def signUp() {
        RegistrationCommand cmd = new RegistrationCommand()
        if (!validateUserData(cmd, params)) {
            return redirect(action:"create")
        }
        boolean captchaValid = simpleCaptchaService.validateCaptcha(params.captcha)
        if (!captchaValid) {
            flash.message="The text entered did not match the image. Please try again"
            return redirect(action:"create")
        }
        if (params.verysecure) {
            flash.message="I hope you are a robot. Otherwise something has gone wrong."
            return redirect(action:"create")
        }
        try {
            userService.register(cmd.toUser())
        } catch (Exception e) {
    		flash.message = e.getMessage()
            log.error e.message, e
   			return redirect(action: "create")
    	}
    	render(view: "successfulregistration", model: [email: cmd.email])
    }

    /**
     * Fetch users' data based what customers are typing. The data populate the source of
     * Autocomplete widgets. The data can be customised but they have to include two mandatory
     * fields as label and value. The two fields are formed from the other ones. For example:
     * label = userRealName (username<email>)
     */
    @Secured(["IS_AUTHENTICATED_FULLY"])
    def fetchUsers() {
        def request = params.request
        def searchTerm = params.search
        if (Integer.parseInt(request) == 1) {
            List users = userService.searchUsers(searchTerm)
            def usersMap = []
            users.each {user ->
                def email = user[0]
                def username = user[1]
                def userRealName = user[2]
                def id = user[3]
                usersMap << [label: "${userRealName} (${username}<${email}>)",
                             value: id,
                             username: username,
                             email: email,
                             userRealname: userRealName]
            }
            render(usersMap as JSON)
        } else {
            def username = params.username
            User user = userService.getUser(username)
            render([user] as JSON)
        }
    }

    /**
     * This controller tries to query the database to get the user who is potentially associated
     * with the fields provided by new users. It is called in the register view where we parse
     * the input values and come up with a Ajax call to UserService in order to look up them
     * into the database.
     *
     * @return JSON string  the query if an user matches with, or an empty string in otherwise.
     */
    @Secured(["IS_AUTHENTICATED_ANONYMOUSLY", "IS_AUTHENTICATED_FULLY"])
    def lookupUser() {
        String query = params?.query
        int column = Integer.parseInt(params?.column)
        User user = userService.lookupUser(query, column)
        String response = ""
        if (user) {
            response = query
        }
        render([response] as JSON)
    }
}
