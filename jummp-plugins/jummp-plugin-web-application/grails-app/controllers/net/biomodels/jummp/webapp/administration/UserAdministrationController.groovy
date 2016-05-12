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
* Grails, Spring Security (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Grails, Spring Security used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.webapp.administration

import grails.plugins.springsecurity.Secured
import grails.converters.JSON
import net.biomodels.jummp.core.JummpException
import net.biomodels.jummp.core.user.UserNotFoundException
import net.biomodels.jummp.core.user.RoleNotFoundException
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.webapp.RegistrationCommand
import net.biomodels.jummp.webapp.EditUserCommand

/**
 * @short Controller for user management.
 *
 * This controller is only useful to administrators and allows to manage all users.
 * There is a list of users which is rendered in a dataTable and allows to modify
 * the users' properties.
 *
 * @author Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 */
@Secured('ROLE_ADMIN')
class UserAdministrationController {
    /**
     * Dependency Injection of RemoteUserService
     */
    def userService
    /**
     * Dependency Injection of SpringSecurityService
     */
    def springSecurityService

    /**
     * Default action showing the DataTable markup
     */
    def index = {
    }

    /**
     * Action returning the DataTable content as JSON
     */
    def dataTableSource = {
        int start = 0
        int length = 10
        if (params.iDisplayStart) {
            start = params.iDisplayStart as int
        }
        if (params.iDisplayLength) {
            length = Math.min(100, params.iDisplayLength as int)
        }
        def dataToRender = [:]
        dataToRender.sEcho = params.sEcho
        dataToRender.aaData = []

        dataToRender.iTotalRecords = userService.getTotalUserCount()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords

        List users = userService.getAllUsers(start, length)
        users.each { user ->
            dataToRender.aaData << [user.id, user.username, user.person.userRealName, user.email, user.person.institution, user.person.orcid, user.enabled, user.accountExpired, user.accountLocked, user.passwordExpired]
        }
        render dataToRender as JSON
    }

    /**
     * Action to enable a given user
     */
    def enable = {
        try {
            def data = [success: userService.enableUser(params.id as Long, Boolean.parseBoolean(params.value))]
            render data as JSON
        } catch (UserNotFoundException e) {
            def data = [error: true, message: e.message]
            render data as JSON
        }
    }

    /**
     * Action to (un)lock a given user
     */
    def lockAccount = {
        try {
            def data = [success: userService.lockAccount(params.id as Long, Boolean.parseBoolean(params.value))]
            render data as JSON
        } catch (UserNotFoundException e) {
            def data = [error: true, message: e.message]
            render data as JSON
        }
    }

    /**
     * Action to (un)expire a given user
     */
    def expireAccount = {
        try {
            def data = [success: userService.expireAccount(params.id as Long, Boolean.parseBoolean(params.value))]
            render data as JSON
        } catch (UserNotFoundException e) {
            def data = [error: true, message: e.message]
            render data as JSON
        }
    }

    /**
     * Action to (un)expire the password of a given user 
     */
    def expirePassword = {
        try {
            def data = [success: userService.expirePassword(params.id as Long, Boolean.parseBoolean(params.value))]
            render data as JSON
        } catch (UserNotFoundException e) {
            def data = [error: true, message: e.message]
            render data as JSON
        }
    }

    /**
     * Action to edit an user
     */
    def show = {
        /*if (!springSecurityService.isAjax(request)) {
            render(template: "/templates/page", model: [link: g.createLink(action: "show", id: params.id), callback: "loadAdminUserCallback"])
            return
        }*/
        [user: userService.getUser(params.id as Long), roles: userService.getAllRoles(), userRoles: userService.getRolesForUser(params.id as Long)]
    }

    /**
     * Action to add a role to a user
     */
    def addRole = { AddRemoveRoleCommand cmd ->
        Map data = [:]
        if (cmd.hasErrors()) {
            data.put("error", g.message(code: "user.administration.userRole.error.general"))
        } else {
            try {
                userService.addRoleToUser(cmd.userId, cmd.id)
                data.put("success", "true")
            } catch (UserNotFoundException e) {
                data.put("error", g.message(code: "user.administration.userRole.error.userNotFound"))
            } catch (RoleNotFoundException e) {
                data.put("error", g.message(code: "user.administration.userRole.error.roleNotFound"))
            }
        }
        render data as JSON
    }

    /**
     * Action to remove a role from a user
     */
    def removeRole = { AddRemoveRoleCommand cmd ->
        Map data = [:]
        if (cmd.hasErrors()) {
            data.put("error", g.message(code: "user.administration.userRole.error.general"))
        } else {
            try {
                userService.removeRoleFromUser(cmd.userId, cmd.id)
                data.put("success", "true")
            } catch (UserNotFoundException e) {
                data.put("error", g.message(code: "user.administration.userRole.error.userNotFound"))
            } catch (RoleNotFoundException e) {
                data.put("error", g.message(code: "user.administration.userRole.error.roleNotFound"))
            }
        }
        render data as JSON
    }

    /**
     * Action to render the view to register a new user as admin
     */
    def register = {
    }

    /**
     * Action to perform the registration of a new user as admin
     */
    def performRegistration = { RegistrationCommand cmd ->
        def data = [:]
        if (cmd.hasErrors()) {
            data.put("error", true)
            data.put("username", resolveErrorMessage(cmd, "username", "User Name"))
            data.put("email", resolveErrorMessage(cmd, "email", "Email"))
            data.put("userRealName", resolveErrorMessage(cmd, "userRealName", "Name"))
        } else {
            try {
                data.put("user", userService.register(cmd.toUser()))
                data.put("success", true)
            } catch (JummpException e) {
                data.clear()
                data.put("error", true)
                data.put("username", e.message)
            }
        }
        render data as JSON
    }

    /**
     * Action for editing non-security relevant user information.
     */
    def editUser = { EditUserCommand cmd ->
        Map data = [:]
        if (cmd.hasErrors()) {
            data.put("error", true)
            data.put("username", resolveErrorMessage(cmd, "username", "Username"))
            data.put("userRealName", resolveErrorMessage(cmd, "userRealName", "Name"))
            data.put("email", resolveErrorMessage(cmd, "email", "Email"))
        } else {
            userService.editUser(cmd.toUser())
            data.put("success", true)
        }
        render data as JSON
    }

    private String resolveErrorMessage(EditUserCommand cmd, String field, String description) {
        if (cmd.errors.getFieldError(field)) {
            switch (cmd.errors.getFieldError(field).code) {
            case "blank":
                return g.message(code: "user.administration.${field}.blank")
            case "email.invalid":
                return g.message(code: "user.administration.${field}.invalid")
            default:
                return g.message(code: "error.unknown", args: [description])
            }
        }
        return null
    }

    /**
     * Resolves the error message for a field error
     * @param cmd The RegistrationCommand for resolving the errors
     * @param field The field to be tested
     * @param description A descriptive name of the field to be passed to unknown errors
     * @return The resolved error message or @c null if there is no error
     */
    private String resolveErrorMessage(RegistrationCommand cmd, String field, String description) {
        if (cmd.errors.getFieldError(field)) {
            switch (cmd.errors.getFieldError(field).code) {
            case "blank":
                return g.message(code: "user.administration.register.error.${field}.blank")
            case "validator.invalid":
                return g.message(code: "user.administration.register.error.${field}.invalid")
            case "email.invalid":
                return g.message(code: "user.administration.register.error.${field}.invalid")
            default:
                return g.message(code: "error.unknown", args: [description])
            }
        }
        return null
    }
}

/**
 * @short Command Object for add/remove role actions
 */
class AddRemoveRoleCommand {
    Long id
    Long userId

    static constraints = {
        id(nullable: false)
        userId(nullable: false)
    }
}

