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
import net.biomodels.jummp.model.ContributionDetails as CD
import net.biomodels.jummp.model.ContributionInvite as CI
import net.biomodels.jummp.model.ContributionRole as CR
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.CommonController
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.security.access.AccessDeniedException

/**
 * @short Controller class for handling the list of contributors.
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
@Secured(["isAuthenticated()"])
class ContributorController extends CommonController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContributorController.class)

    static allowedMethods = [update: "POST"]

    def userService
    def modelService
    def mailService
    def contributorService

    private final Random random = new Random(System.currentTimeMillis())

    //@Secured(["ROLE_ADMIN", "ROLE_CURATOR"])
    def init() {
        // create the first contributors based on the existing model revisions
        Map parameters = parseParameters()
        Revision revision = parameters.get("revision") as Revision
        Map result = [:]
        Map<String, CTC> contributors = contributorService.createFirstContributors(revision)
        result.put("contributors", contributors)
        String message = ""
        String htmlBasedStringOfContributors = ""
        if (contributors) {
            Map<String, CD> savedContributors = contributorService.saveFirstContributors(contributors, revision)
            if (0 == savedContributors?.size()) {
                message = "Cannot initialise the first contributors."
                htmlBasedStringOfContributors = ""
            } else {
                StringBuilder sb = new StringBuilder()
                for (CTC cont : contributors.values()) {
                    String htmlString = g.render(template: "/contributor/showContributor",
                        plugin: "jummp-plugin-web-application",
                        model: [cont: cont, serverURL: serverURL, roles: contributorService.roles])
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
        String id = params.get("id").decodeHTML()
        String username = userService?.username

        Model model = null
        try {
            // this method has already handled the dot (.) between the model id and revision number
            model = modelService.getModel(id)
        } catch (AccessDeniedException adException) {
            LOGGER.debug("Tried to access $id but got this exception: ${adException.getMessage()}")
            forward(controller: "errors", action: "error403")
            return
        } finally {
            LOGGER.info("$username has accessed $id to manage contributors.")
        }

        if (!model) {
            LOGGER.debug("An error has happened when accessing $id to manage contributors by $username.")
            forward(controller: "errors", action: "error500")
            return
        }

        // Handle the id param to split the model and revision id
        Map map = doAnalyseAndExtractParameters(id, model)
        String modelId = map["modelId"] as String
        Integer revisionNumber = map['revisionNumber'] as Integer
        Revision revision = map["revision"] as Revision

        String message = ""
        Map<String, CTC> contributors = getContributors(revision)
        List contributorEmailList = contributors.values().collect { it.user.email }
        String currentUserEmail = userService.getEmailAddress()
        String currentUsername = userService.username
        String currentUserRealName = userService.getRealName(currentUsername)
        Map retMap = [modelId: modelId, revisionNumber: revisionNumber, revision: revision,
                      authors: params?.authors, message: message,
                      contributorEmailList: contributorEmailList,
                      roles: contributorService.roles, contributors: contributors,
                      serverURL: serverURL,
                      currentUsername: currentUsername,
                      currentUserEmail: currentUserEmail,
                      currentUserRealName: currentUserRealName]
        render(view: "manage", model: retMap)
    }

    private Map<String, Object> doAnalyseAndExtractParameters(final String id, final Model model) {
        Map<String, Object> map = [:]
        String modelId = id
        Integer revisionNumber = 0
        if (id.indexOf(".") > 0) {
            // having the revision number
            modelId = id.substring(0, id.indexOf("."))
            String revisionId = id.substring(id.lastIndexOf(".") + 1)
            revisionNumber  = revisionId.toInteger()
        }

        Set<Revision> revisions = model.revisions
        ArrayList<Integer> revisionNumbers = revisions.sort { r1, r2 ->
                r2.revisionNumber <=> r1.revisionNumber }*.revisionNumber
        if (revisionNumber == 0) {
            // Get the latest revision
            // Sort the revisions descending by the revision number
            revisionNumber = revisionNumbers[0]
        } else {
            // Get the revision which revision number equals revisionNumber
            // do nothing
        }

        // TODO: handle 1 <= revisionNumer <= max
        map["modelId"] = modelId
        map["revisionNumber"] = revisionNumber

        Revision revision = revisions.find { it.revisionNumber == revisionNumber }
        map["revision"] = revision
        return map
    }

    private static Map<String, CTC> getContributors(Revision revision) {

        Model model = revision.model
        if (!model) { return null }
        Map<String, CTC> contributorMap = [:]
        List revisions = model.revisions.toList()

        Set authors = revisions*.owner?.collect { it.username }?.toSet()

        List details = CD.findAllByRevision(revision)
        for (CD detail: details) {
            String username = detail.contributor.username
            boolean locked = username in authors
            CTC ctc = new CTC(user: detail.contributor,
                role: detail.role, person: detail.contributor.person, locked: locked)
            contributorMap.put(username, ctc)
        }

        contributorMap
    }

    def add() {
        String modelId = params["modelId"]?.decodeHTML()
        Integer revisionNumber = params.getInt("revisionNumber")
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
        String htmlString = g.render(template: "/contributor/showContributor",
            plugin: "jummp-plugin-web-application",
            model: [cont: ctc, serverURL: serverURL, roles: contributorService.roles])
        result.put("htmlBasedStringForNewContributor", htmlString)
        result.put("message", "The data has been updated successfully!")
        render(result as JSON)
    }

    def handleInviteResponse() {
        String refCode = params["ref"]?.decodeHTML()
        String op = params["op"]?.decodeHTML()
        Map retMap = [:]
        retMap.put("reference", refCode)
        retMap.put("inviteeResponse", op)
        String msgLog
        String msgUser
        CI ci = CI.findWhere(reference: refCode)

        if (!ci) {
            msgLog = "The invitation with the reference ${refCode} could be expired or invalid."
            msgUser = msgLog
            retMap.putAll([msgLog: msgLog, msgUser: msgUser])
        } else {
            User currentUser = userService.currentUser
            if (currentUser.email != ci.inviteeEmail) {
                msgLog = "The user (${currentUser.email}) shouldn't have the access of the invite ${refCode}."
                msgUser = "Unfortunately, you are not allowed to perform this operation. You're an user registered with another email!"
            } else {
                if ("accept" == op) {
                    retMap.putAll(contributorService.processAccept(ci))
                } else if ("reject" == op) {
                    retMap.putAll(contributorService.processReject(ci))
                } else {
                    msgLog = "Please stop cheating our system. Thanks!"
                    msgUser = msgLog
                }
            }
            if (!retMap.containsKey("msgLog")) { retMap.put("msgLog", msgLog) }
            if (!retMap.containsKey("msgUser")) { retMap.put("msgUser", msgUser) }
            if (retMap.get("msgLog")) {
                LOGGER.debug(retMap.get("msgLog") as String)
                println(retMap["msgLog"]) // for K8s log
            }
        }
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
        String roleName = params["role"]?.decodeHTML()
        CR role = CR.findByName(roleName)
        String refCode = String.valueOf(random.nextInt()) + params["inviterUsername"]?.decodeHTML()
        refCode = refCode.encodeAsMD5()
        result.putAll([role: role, refCode: refCode, serverURL: serverURL] as Map)

        String msg = ""
        String subjectLine = "${inviterName} invited you to join your submission in BioModels as as a ${roleName.toLowerCase()}"
        String howtoAction = "Send"
        // 1. Create a record in the contribution_invite table
        User inviter = User.findByUsername(inviterUsername)
        CI ci = CI.findWhere(inviter: inviter, inviteeEmail: inviteeEmail, revision: revision, role: role)
        Map r = contributorService.findOrCreateInvite(ci, inviterName, inviter, inviteeEmail,
                                    howtoAction, refCode, role, revision)
        result.putAll(r)

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
            println "$message: ${exception.toString()}"
            LOGGER.error(message, exception)
            throw exception
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

    private Map parseParameters() {
        User contributor = null
        if (params.containsKey("usernameAndEmail")) {
            String usernameAndEmail = params.get("usernameAndEmail").decodeHTML()
            String[] parts = usernameAndEmail.split(", ")
            String username = parts[0]
            String email = parts[1]
            contributor = User.findByUsernameAndEmail(username, email)
        }

        String modelId = params.get("modelId").decodeHTML()
        String revisionNumber = params.getInt("revisionNumber")
        Model model = modelService.getModel("$modelId.$revisionNumber")
        Revision revision = Revision.findByModelAndRevisionNumber(model, revisionNumber)

        [contributor: contributor, revision: revision, modelId: modelId,
         revisionNumber: revisionNumber, revisionIdentifier: "$modelId.$revisionNumber"]
    }
}
