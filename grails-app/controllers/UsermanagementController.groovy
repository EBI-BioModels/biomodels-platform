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





import net.biomodels.jummp.webapp.RegistrationCommand
import net.biomodels.jummp.webapp.EditUserCommand
import net.biomodels.jummp.webapp.UpdatePasswordCommand
import net.biomodels.jummp.webapp.ResetPasswordCommand
import grails.plugins.springsecurity.Secured
import net.biomodels.jummp.plugins.security.Person

/*
* @short Controller for managing user registrations
* @author Raza Ali <raza.ali@ebi.ac.uk>
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
    def create = {
        render view: "register", model: [postUrl: "", flashMessage: checkForMessage(),
    									validationErrorOn: checkForErrorBean()]
    }

    @Secured(["isAuthenticated()"])
    def edit = {
    	String user = springSecurityService.principal.username
        render view: "edit", model: [postUrl          : "", flashMessage: checkForMessage(),
                                     validationErrorOn: checkForErrorBean(),
    								user: userService.getUser(user),
    								notificationPermissions: notificationService.getNotificationPermissions(user)]
    }

    @Secured(["isAuthenticated()"])
    def editPassword = {
        render view: "editPassword", model: [postUrl          : "", flashMessage: checkForMessage(),
                                             validationErrorOn: checkForErrorBean(),
    										user: userService.getUser(springSecurityService.principal.username)]
    }

    @Secured(["isAuthenticated()"])
    def show = {
    	String user = springSecurityService.principal.username
        render view: "show", model: [postUrl          : "", flashMessage: checkForMessage(),
                                     validationErrorOn: checkForErrorBean(),
    								user: userService.getUser(user),
    								notificationPermissions: notificationService.getNotificationPermissions(user)]
    }

    @Secured(["isAnonymous()"])
    def forgot = {
        render view: "forgot", model: [postUrl: "", flashMessage: checkForMessage(),
    								validationErrorOn: checkForErrorBean()]
    }

    @Secured(["isAnonymous()"])
    def passwordreset = {
    	if (params.id) {
    		flash.hashCode=params.id
    		redirect action: reset
    	}
    	else {
    		redirect action: forgot;
    	}
    }

    /**
    * Password reset based on the unique code sent to the user
    **/
    @Secured(["isAnonymous()"])
    def reset = {
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
    def editUser = {
        EditUserCommand cmd = new EditUserCommand()
        if (!validateUserData(cmd, params)) {
            return redirect(action:"edit")
        }
        try {
            def user = cmd.toUser()
        	userService.editUser(user)
        	notificationService.updatePreferences(cmd.getPreferences(user))
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
    def newPassword = {
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
    def updatePassword = {
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
    **/
    def requestPassword = {
        String username = params.username
        boolean usernameExists = true
        if (username) {
            try {
                userService.requestPassword(username)
            }
            catch(Exception e) {
                log.warn(e.message, e)
                usernameExists = false
            }
            if (usernameExists) {
                flash.message = "Thank you. Please check the email associated with ${username}'s account"
            } else {
                flash.message = "Username ${username} does not exist."
            }
        }
        else {
            flash.message = "Please provide a username.";
        }
        redirect(action:"forgot")
    }

     /**
     * Performs validation on the captcha, ensures that the empty security parameter is not
     * filled (as is commonly done by robots), validates the command object and then uses
     * the user service to create a user. If an error occurs at any point, the method redirects
     * to create action and sends the user a helpful message.
     */
    def signUp = {
    	boolean captchaValid = simpleCaptchaService.validateCaptcha(params.captcha)
    	if (!captchaValid) {
    		flash.message="The text entered did not match the image. Please try again"
    		return redirect(action:"create")
    	}
    	if (params.verysecure) {
    		flash.message="I hope you are a robot. Otherwise something has gone wrong."
    		return redirect(action:"create")
    	}
    	RegistrationCommand cmd=new RegistrationCommand()
    	if (!validateUserData(cmd, params)) {
    		return redirect(action:"create")
    	}
        try
    	{
    		userService.register(cmd.toUser())
    	}
    	catch(Exception e) {
    		flash.message=e.getMessage()
            log.error e.message, e
   			return redirect(action:"create")
    	}
    	render view: "successfulregistration"
    }
}
