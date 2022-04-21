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
 */

package net.biomodels.jummp.plugins.security

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import net.biomodels.jummp.core.model.ContributorTransportCommand as CTC
import net.biomodels.jummp.core.model.InviteState as IS
import net.biomodels.jummp.webapp.CommonController
import net.biomodels.jummp.model.ContributionDetails as CD
import net.biomodels.jummp.model.ContributionInvite as CI
import net.biomodels.jummp.model.ContributionRole as CR
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.Revision
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.OptimisticLockingFailureException

/**
 * @short Controller class for interacting with user.
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
@Secured(["isAuthenticated()"])
class ContributorController extends CommonController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContributorController.class)

    static allowedMethods = [update: "POST"]

    /**
     * Dependency Injection of Spring Security Service
     */
    def springSecurityService
    /**
     * Dependency Injection of Team Service
     */
    def teamService
    def userService
    def modelService
    def mailService
    private final Random random = new Random(System.currentTimeMillis())

    @Secured(["ROLE_ADMIN", "ROLE_CURATOR"])
    def init() {
        // create the first contributors based on the existing model revisions
        Map parameters = parseParameters()
        Revision revision = parameters.get("revision") as Revision
        Map result = [:]
        Map<String, CTC> contributors = createFirstContributors(revision)
        result.put("contributors", contributors)
        String message = ""
        String htmlBasedStringOfContributors = ""
        if (contributors) {
            Map<String, CD> savedContributors = saveFirstContributors(contributors, revision)
            if (0 == savedContributors?.size()) {
                message = "Cannot initialise the first contributors."
                htmlBasedStringOfContributors = ""
            } else {
                List<String> roles = CR.getAll().collect {
                    it.name
                }.sort()
                StringBuilder sb = new StringBuilder()
                for (CTC cont : contributors.values()) {
                    String htmlString = g.render(template: "/contributor/showContributor",
                        plugin: "jummp-plugin-web-application",
                        model: [cont: cont, serverURL: serverURL, roles: roles])
                    sb.append(htmlString)
                }
                htmlBasedStringOfContributors = sb.toString()
                message = "Initialised the contributors successfully."
            }
        } else {
            htmlBasedStringOfContributors = ""
            message = "There is no contributor for this model revision."
        }
        result.put("htmlBasedStringOfContributors", htmlBasedStringOfContributors)
        result.put("message", message)

        render(result as JSON)
    }

    def manage() {
        String modelId = params.get("id").decodeHTML()
        String revisionNumber = params.get("format").decodeHTML()
        String message = ""
        // The roles are ordered by the permission in ascending
        List<String> roles = CR.getAll().collect { it.name }.sort { it }
        Map<String, CTC> contributors = getContributors(modelId, revisionNumber)
        List contributorEmailList = contributors.values().collect { it.user.email }
        String currentUserEmail = userService.getEmailAddress()
        String currentUsername = userService.username
        String currentUserRealName = userService.getRealName(currentUsername)
        Map retMap = [modelId: modelId, revisionNumber: revisionNumber,
                      authors: params?.authors, message: message,
                      contributorEmailList: contributorEmailList,
                      roles: roles, contributors: contributors,
                      serverURL: serverURL,
                      currentUsername: currentUsername,
                      currentUserEmail: currentUserEmail,
                      currentUserRealName: currentUserRealName]
        render(view: "manage", model: retMap)
    }

    private Map getContributors(String modelId, String revisionNumber) {
        Model model = modelService.getModel("$modelId.$revisionNumber")
        if (!model) { return null }
        Map contributorMap = [:]
        CTC ctc
        List revisions = model.revisions.toList()
        Revision revision = revisions.find { revisionNumber == it.revisionNumber.toString() }
        Set authors = revisions*.owner?.collect { it.username }.toSet()
        List details = CD.findAllByRevision(revision)
        for (CD detail: details) {
            String username = detail.contributor.username
            boolean locked = username in authors
            ctc = new CTC(user: detail.contributor, role: detail.role,
                                person: detail.contributor.person, locked: locked)
            contributorMap.put(username, ctc)
        }

        contributorMap
    }

    def add() {
        String modelId = params["modelId"]?.decodeHTML()
        int revisionNumber = params.getInt("revisionNumber")
        Model model = modelService.getModel("$modelId.$revisionNumber")
        if (!model) { return null }
        String email = params["email"]?.decodeHTML()
        Map result = [:]
        result["email"] = email
        User user = userService.lookupUser(email, 1)
        CTC ctc = null
        if (user) {
            CR modellerRole = CR.findByName("Modeller")
            Revision revision = Revision.findByModelAndRevisionNumber(model, revisionNumber)
            // create a new record to capture the association among user, model revision and role
            CD details = new CD(contributor: user, revision: revision, role: modellerRole)
            if (details.save(flush: true)) {
                LOGGER.debug("Sent a contribution invite (user: ${user.username}) successfully")
                println("Sent a contribution invite (user: ${user.username}) successfully")
            } else {
                println("Failed: ${details.allErrors().toString()}")
            }
            // create a new record in the contribution_invite table
            ctc = new CTC(user: user, role: modellerRole,
                person: user.person, locked: false)
        }
        result.put("newCont", ctc)
        List<String> roles = CR.getAll().collect {
            it.name
        }.sort()
        String htmlString = g.render(template: "/contributor/showContributor",
            plugin: "jummp-plugin-web-application",
            model: [cont: ctc, serverURL: serverURL, roles: roles])
        result.put("htmlBasedStringForNewContributor", htmlString)
        result.put("message", "The data has been updated successfully!")
        render(result as JSON)
    }

    def handleInviteResponse() {
        String refCode = params["ref"]?.decodeHTML()
        String op = params["op"]?.decodeHTML()
        CI ci = CI.findWhere(reference: refCode)
        String msgLog
        String msgUser
        String modelId = ""
        if ("accept" == op) {
            if (ci) {
                String inviteeEmail = ci.inviteeEmail
                modelId = ci.revision.model.submissionId
                User inviteeAccount = User.findByEmail(inviteeEmail)
                if (inviteeAccount) {
                    CD cd = CD.findWhere(contributor: inviteeAccount, revision: ci.revision, role: ci.role)
                    if (!cd) {
                        cd = new CD(contributor: inviteeAccount, revision: ci.revision, role: ci.role)
                        boolean saved = cd.save(flush: true, insert: true)
                        if (saved) {
                            msgLog = "Added the user (${inviteeEmail}) as the ${ci.role.name} for " +
                                "the model ${modelId}"
                            msgUser = "You have joined your submission with the identifier ${modelId} in BioModels."
                            updateCI(ci, "accept")
                        } else {
                            msgLog = "There has been an error why persisting the user (${inviteeEmail}) as " +
                                "the ${ci.role.name} for the model ${ci.revision.model.submissionId}"
                            msgUser = "Oops! There has been an error. Please try to open the invitation link or contact us!"
                        }
                    } else {
                        String modelURL = createLink(controller: "model", action: "show", id: modelId)
                        modelURL = '<a href="' + modelURL + '" target="_blank">' + modelId + '</a>'
                        msgLog = "The user (${inviteeEmail}) as the ${ci.role.name} joined the model ${modelId}."
                        msgUser = "You already accepted the invitation. Access your model ${modelURL}."
                    }
                } else {
                    msgLog = "Sorry, we couldn't find any user registered with the email ${inviteeEmail}"
                    msgUser = "Sorry, we couldn't find any user registered with the email ${inviteeEmail}"
                }
            } else {
                msgLog = "The invitation is invalid due to expired, cancelled or rejected."
                msgUser = "The invitation is invalid due to expired, cancelled or rejected."
            }
        } else if ("reject" == op) {
            if (ci) {
                msgLog = "Thank you for your response! We are always happy to support you in the near future."
                msgUser = msgLog
                updateCI(ci, "reject")
                // TODO: update the contributor list either remove this user or expire the invitation: accepted and not allow to revert
            } else {
                msgLog = "The invitation is invalid due to expired, cancelled or rejected."
                msgUser = "The invitation is invalid due to expired, cancelled or rejected."
            }
        } else {
            msgLog = "Please stop cheating our system. Thanks!"
            msgUser = "Please stop cheating our system. Thanks!"
        }
        Map retMap = [:]
        retMap.put("reference", refCode)
        retMap.put("inviteeResponse", op)
        retMap.put("msgLog", msgLog)
        retMap.put("msgUser", msgUser)
        retMap.put("modelId", modelId)
        LOGGER.debug(msgLog)
        println(msgLog)
        render(view: "handleInviteResponse", model: retMap)
    }

    def invite() {
        String modelId = params["modelId"]?.decodeHTML()
        int revisionNumber = params.getInt("revisionNumber")
        Model model = modelService.getModel("$modelId.$revisionNumber")
        Revision revision = model.revisions.find { it.revisionNumber == revisionNumber }
        if (!model) { return null }
        String inviterEmail = params["inviterEmail"]?.decodeHTML()
        String inviterName = params["inviterName"]?.decodeHTML()
        String inviterUsername = params["inviterUsername"]?.decodeHTML()
        String inviteeEmail = params["inviteeEmail"]?.decodeHTML()
        Map result = [:]
        result["inviterEmail"] = inviterEmail
        result["inviterUsername"] = inviterUsername
        result["inviterName"] = inviterName
        result["inviteeEmail"] = inviteeEmail
        String role = params["role"]?.decodeHTML()
        CR contributionRole = CR.findByName(role)
        String refCode = String.valueOf(random.nextInt()) + params["inviterUsername"]?.decodeHTML()
        refCode = refCode.encodeAsMD5()
        result.put("role", role)
        result.put("refCode", refCode)
        result.put("serverURL", params["serverURL"]?.decodeHTML())

        String msg = ""
        String subjectLine = "${inviterName} invited you to join your submission in BioModels as as a ${role.toLowerCase()}"
        String emailHeading = "You are invited!"
        String howtoAction = "Send"
        // 1. Create a record in the contribution_invite table
        User inviter = User.findByUsername(inviterUsername)
        CI ci = CI.findWhere(inviter: inviter, inviteeEmail: inviteeEmail, revision: revision, role: contributionRole)
        if (ci) {
            switch (ci.state) {
                case IS.ACCEPTED:
                    msg = "There has been an user in our system registed with the email ${inviteeEmail}."
                    // do nothing
                    break
                case IS.PENDING:
                    msg = "A gentle reminder has been sent to the email ${inviteeEmail}."
                    subjectLine = "${inviterName} is still waiting for you to join your submission in BioModels as a ${role.toLowerCase()}"
                    emailHeading = "In case you missed it..."
                    howtoAction = "Remind"
                    break
                case IS.CANCELLED:
                    msg = "An invitation has been sent to the email ${inviteeEmail}."
                    howtoAction = "Resend"
                    break
                case IS.REJECTED:
                    msg = "An invitation has been sent to the email ${inviteeEmail}."
                    howtoAction = "Resend"
                    break
                case IS.RESENT:
                    msg = "A gentle reminder has been sent to the email ${inviteeEmail}."
                    subjectLine = "${inviterName} is still waiting for you to join your submission in BioModels as a ${role.toLowerCase()}"
                    emailHeading = "In case you missed it..."
                    howtoAction = "Remind"
                    break
                default:
                    msg = "An invitation has been sent to the email ${inviteeEmail}."
                    howtoAction = "Send"
                    break
            }
            ci.reference = refCode
            ci.dateSent = new Date()
            if ("Remind" == howtoAction) {
                ci.state = IS.RESENT
            } else {
                ci.state = IS.PENDING
            }

            if (ci.merge(flush: true)) {
                msg += " The invitation has been updated or the reminder has been sent successfully."
            } else {
                msg += " The invitation has been updated or the reminder has been sent unsuccessfully. The cause is "
                msg += "${ci.errors.toString()}"
            }
        } else {
            howtoAction = "Send"
            ci = new CI(inviter: inviter, inviteeEmail: inviteeEmail, reference: refCode,
                revision: revision, role: contributionRole, dateSent: new Date(), state: IS.PENDING)
            if (ci.save(insert: true, flush: true)) {
                msg = "An invitation has been sent to the email ${inviteeEmail}. "
                msg += "Created a contribution invite (user: ${inviteeEmail}) successfully."
                result.put("contribution_invite_id", ci.id.toString())
            } else {
                msg = "Failed: ${ci.errors.toString()}"
            }
        }

        result.put("message", msg)
        result.put("subjectLine", subjectLine)
        result.put("howtoAction", howtoAction)
        result.put("emailHeading", emailHeading)

        // 2. Send an email having instructions to the invited contributor
        String htmlBasedContent = g.render(template: "/contributor/inviteEmailTemplate",
            plugin: "jummp-plugin-web-application", model: result)
        mailService.sendMail {
            to inviteeEmail
            from inviterEmail
            subject subjectLine
            html htmlBasedContent
        }

        LOGGER.debug(msg)
        println(msg)

        render(result as JSON)
    }

    def updateRole() {
        Map result = [:]
        String message = "Under construction"

        String usernameAndEmail = params.get("usernameAndEmail").decodeHTML()
        String[] parts = usernameAndEmail.split(", ")
        String username = parts[0]
        String email = parts[1]
        User contributor = User.findByUsernameAndEmail(username, email)

        String modelId = params.get("modelId")
        String revisionNumber = params.get("revisionNumber")
        Model model = modelService.getModel("$modelId.$revisionNumber")
        Revision revision = Revision.findByModelAndRevisionNumber(model, revisionNumber)

        String newRoleName = params.get("newRole").decodeHTML()
        CR newRole = CR.findByName(newRoleName)

        CD details = CD.findByContributorAndRevision(contributor, revision, [locked: true])
        try {
            details?.delete()
            details = new CD(contributor: contributor, revision: revision, role: newRole)
            if (details.save(flush: true)) {
                message = "Update the new role for this contributor successfully."
            } else {
                message = "An error has happened while trying to update the role for this contributor."
            }
        } catch (OptimisticLockingFailureException exception) {
            throw exception
            LOGGER.error(message, exception)
            println "$message: ${exception.toString()}"
        }
        result.put("message", message)
        render(result as JSON)
    }

    def remove() {
        Map result = [:]
        String message = "In progress"
        Map parsedParameters = parseParameters()
        result.putAll(parsedParameters)
        String revisionIdentifier = parsedParameters["revisionIdentifier"]
        User contributor = parsedParameters["contributor"]
        Revision revision = parsedParameters["revision"]
        CD details = CD.findByContributorAndRevision(contributor, revision, [locked: true])
        try {
            details?.delete(flush: true)
            details = CD.findByContributorAndRevision(contributor, revision, [locked: true])
            if (details) {
                message = """\
Remove the contributor \
${contributor.person.userRealName} (${contributor.username}, ${contributor.email}) \
from the model ${revisionIdentifier} unsuccessfully."""
            } else {
                message = """\
The contributor ${contributor.person.userRealName} (${contributor.username}, ${contributor.email}) \
from the model ${revisionIdentifier} has been removed successfully."""
            }
            LOGGER.debug(message)
        } catch (OptimisticLockingFailureException exception) {
            throw exception
            message = """\
An error happened when removing the contributor \
${contributor.person.userRealName} (${contributor.username}, ${contributor.email}) \
from the model ${revisionIdentifier}."""
            LOGGER.error(message, exception)
            println "$message: ${exception.toString()}"
        }
        result.put("message", message)
        render(result as JSON)
    }

    private Map createFirstContributors(final Revision revision) {
        Map<String, CTC> contributors = new HashMap<>()
        CTC ctc = new CTC(user: revision.owner, role: CR.findByName("Curator"),
            person: revision.owner.person, locked: true)
        //List details = CD.findAllByRevisionInList(revisions)
        contributors.put(revision.owner.username, ctc)
        contributors
    }

    private Map parseParameters() {
        User contributor = null
        if (params.containsKey("usernameAndEmail")) {
            String usernameAndEmail = params.get("usernameAndEmail").decodeHTML()
            String[] parts = usernameAndEmail.split(", ")
            String username = parts[0]
            String email = parts[1]
            contributor = User.findByUsernameAndEmail(username, email)
        }

        String modelId = params.get("modelId")
        String revisionNumber = params.get("revisionNumber")
        Model model = modelService.getModel("$modelId.$revisionNumber")
        Revision revision = Revision.findByModelAndRevisionNumber(model, revisionNumber)

        [contributor: contributor, revision: revision, modelId: modelId, revisionNumber: revisionNumber,
         revisionIdentifier: "$modelId.$revisionNumber"]
    }

    private Map saveFirstContributors(final Map<String, CTC> contributors, final Revision revision) {
        Map result = [:]
        for (CTC ctc: contributors.values()) {
            CD cd = CD.findOrSaveWhere(contributor: ctc.user, revision: revision, role: ctc.role)
            if (cd.save(flush: true)) {
                println ctc.toString()
                result.put(ctc.toString(), cd)
            }
        }
        result
    }

    private CI updateCI(CI ci, String userResponse) {
        ci.dateCompleted = new Date()
        switch (userResponse) {
            case "accept":
                ci.state = IS.ACCEPTED
                break
            case "reject":
                ci.state = IS.REJECTED
                break
        }
        ci.merge(flush: true)
        return ci
    }
}
