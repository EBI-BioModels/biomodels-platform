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
import net.biomodels.jummp.CommonController
import net.biomodels.jummp.core.model.ContributorTransportCommand as CTC
import net.biomodels.jummp.model.ContributionDetails as CD
import net.biomodels.jummp.model.ContributionDetailsWithoutInvite as CDWI
import net.biomodels.jummp.model.ContributionInvite as CI
import net.biomodels.jummp.model.ContributionRole as CR
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.utils.FileHelper
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
    def mailingService
    def contributorService

    private final Random random = new Random(System.currentTimeMillis())

    def init() {
        // create the first contributors based on the existing model revisions
        Map parameters = parseParameters()
        Revision revision = parameters.get("revision") as Revision
        Map result = [:]
        Map<String, CTC> contributors = contributorService.createFirstContributors(revision)
        result.put("contributors", contributors)
        String message
        String htmlBasedStringOfContributors
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

    @Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
    def role() {
        List<CR> roleList = CR.all
        Map mapRoles = [:]
        roleList.each {
            mapRoles.put(it.name, it.description)
        }
        render(mapRoles as JSON)
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
        String modelId = params.get("id")
        String revisionId = params.get("revisionId")
        Map map = doAnalyseAndExtractParameters(model, modelId, revisionId)
        Integer revisionNumber = map['revisionNumber'] as Integer
        Revision revision = map["revision"] as Revision

        String message = ""
        Map<String, List<CTC>> contributors = contributorService.getContributors(revision)
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

    private static Map<String, Object> doAnalyseAndExtractParameters(final Model model, final String modelId, final
            String revisionId = null) {
        Map<String, Object> map = [:]
        Integer revisionNumber = 0
        if (revisionId) {
            revisionNumber = revisionId.toInteger()
        } else {
            // Get the latest revision
            // Sort the revisions descending by the revision number
            Set<Revision> revisions = model.revisions
            ArrayList<Integer> revisionNumbers = revisions.sort { r1, r2 ->
                    r2.revisionNumber <=> r1.revisionNumber }*.revisionNumber
            revisionNumber = revisionNumbers[0]
        }

        // TODO: handle 1 <= revisionNumber <= max
        map["modelId"] = model.submissionId
        map["revisionNumber"] = revisionNumber

        Revision revision = model.revisions.find { it.revisionNumber == revisionNumber }
        map["revision"] = revision
        return map
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
            String roleName = params["role"]?.decodeHTML() ?: "Modeller"
            CR selectedRole = CR.findByName(roleName) ?: CR.findByName("Modeller")
            Revision revision = Revision.findByModelAndRevisionNumber(model, revisionNumber)
            // create a new record to capture the association among user, model revision and role
            CD details = new CD(contributor: user, revision: revision, role: selectedRole)
            if (details.save(flush: true)) {
                LOGGER.info("Saved contributor record for ${user.username} on ${modelId}.${revisionNumber}")
                if (!mailingService) {
                    LOGGER.error("mailingService is null — skipping contributor notification email to ${user.email}")
                } else {
                    def inviter = userService.currentUser
                    String inviterName = inviter?.person?.userRealName ?: "A BioModels curator"
                    String inviterEmail = inviter?.email ?: ""
                    String recipientName = user.person?.userRealName ?: user.username
                    String modelLink = "${serverURL}/${modelId}"
                    String subjectLine = "${inviterName} has added you as a contributor on BioModels"
                    String htmlBody = """
<div style="background-color:#f4f4f4;margin:0;padding:32px 0;font-family:Arial,Helvetica,sans-serif;color:#333333;">
  <div style="max-width:620px;margin:0 auto;background-color:#ffffff;border-radius:4px;overflow:hidden;box-shadow:0 2px 6px rgba(0,0,0,0.10);">
    <div style="background-color:#ED6B21;height:5px;"></div>
    <div style="background-color:#072C55;padding:24px 32px 20px;">
      <div style="font-size:22px;font-weight:bold;color:#ffffff;letter-spacing:0.5px;">BioModels</div>
      <div style="font-size:12px;color:rgba(255,255,255,0.75);margin-top:4px;letter-spacing:0.3px;">Laboratory for Systems Medicine &bull; University of Florida</div>
    </div>
    <div style="padding:32px;font-size:15px;line-height:1.7;color:#333333;">
      <p style="margin:0 0 16px;">Dear ${recipientName},</p>
      <p style="margin:0 0 16px;">${inviterName} has added you as a <strong>${selectedRole.name}</strong> contributor to the following BioModels submission:</p>
      <p style="margin:0 0 16px;"><a href="${modelLink}" style="color:#0F5CB1;">${modelId}</a></p>
      <p style="margin:0 0 16px;">You can view the model and your contribution details by visiting the link above.</p>
      <p style="margin:0 0 16px;">Kind regards,<br/><strong>The BioModels Team</strong><br/>
        <a href="${serverURL}" style="color:#0F5CB1;">${serverURL}</a>
      </p>
    </div>
    <hr style="border:none;border-top:1px solid #e8e8e8;margin:0;"/>
    <div style="background-color:#f8f8f8;padding:20px 32px;font-size:12px;color:#777777;line-height:1.6;">
      You are receiving this email because you have been added as a contributor on
      <a href="${serverURL}" style="color:#0F5CB1;">BioModels</a>.
      This is an automatically generated email &mdash; replies are not monitored.
    </div>
  </div>
</div>"""
                    try {
                        LOGGER.info("Sending contributor notification email to ${user.email}")
                        mailingService.send([to: user.email, subject: subjectLine, html: htmlBody, replyTo: inviterEmail])
                        LOGGER.info("Contributor notification email sent successfully to ${user.email}")
                    } catch (Exception e) {
                        LOGGER.error("Failed to send contributor notification email to ${user.email}: ${e.message}")
                    }
                }
            } else {
                LOGGER.error("Failed to save contributor record for ${user.username}: ${details.errors}")
            }
            // create a new record in the contribution_invite table
            ctc = new CTC(user: user, role: selectedRole,
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

    def addWithoutInvitation() {
        String modelId = params["modelId"]?.decodeHTML()
        Integer revisionNumber = params.getInt("revisionNumber")
        Model model = modelService.getModel("$modelId.$revisionNumber")
        if (!model) { return null }
        Revision revision = Revision.findByModelAndRevisionNumber(model, revisionNumber)
        if (!revision) { return null }
        String displayName = params["displayName"]?.decodeHTML()
        String email = params["email"]?.decodeHTML()
        String orcid = params["orcid"]?.decodeHTML()
        String affiliation = params["affiliation"]?.decodeHTML()
        String roleName = params["role"]?.decodeHTML()
        Map result = [:]
        result["displayName"] = displayName
        result["email"] = email
        result["orcid"] = orcid
        result["affiliation"] = affiliation

        // SAVE INPUT TO DB
        CR role = CR.findByName(roleName)
        CDWI cDWI = new CDWI(displayName: displayName, email: email, revision: revision, role: role)
        if (affiliation) { cDWI.affiliation = affiliation }
        if (orcid) { cDWI.orcid = orcid }
        if (!cDWI.save()) {
            LOGGER.error("""An occurred when saving [$displayName, $email, $orcid, $modelId, $revisionNumber, \
${role.name}] into the database due to ${cDWI.errors.toString()}.""")
            return false
        }
        // RENDER THE DATA TO VIEW
        // create a temporarily CTC object
        User user = contributorService.createDummyUserPerson(displayName, email, orcid)
        CTC ctc = new CTC(user: user, role: role, person: user.person, locked: false, external: true)
        String htmlString = g.render(template: "/contributor/showContributor",
            plugin: "jummp-plugin-web-application",
            model: [cont: ctc, email: email, displayName: displayName, orcid: orcid, affiliation: affiliation,
                    serverURL: serverURL, roles: contributorService.roles])
        result.put("htmlBasedStringForNewContributor", htmlString)
        result.put("message", "The contributor has been added successfully!")
        render(result as JSON)
    }

    def handleInviteResponse() {
        String refCode = params["ref"]?.decodeHTML()
        String op = params["op"]?.decodeHTML()
        Map retMap = [reference: refCode, op: op]

        CI ci = CI.findWhere(reference: refCode)
        if (!ci) {
            retMap.put("msgUser", "This invitation link has expired or is no longer valid.")
            retMap.put("confirmed", true)
            render(view: "handleInviteResponse", model: retMap)
            return
        }

        if (request.method == "GET") {
            // Show confirmation page only — do NOT process the action.
            // Security scanners follow GET links; the action must require a deliberate POST.
            retMap.put("inviterName", ci.inviter?.person?.userRealName ?: ci.inviter?.username)
            retMap.put("confirmed", false)
            render(view: "handleInviteResponse", model: retMap)
            return
        }

        // POST — user deliberately clicked Accept or Decline on the confirmation page
        String msgLog = ""
        String msgUser = ""
        User currentUser = userService.currentUser
        if (currentUser.email != ci.inviteeEmail) {
            msgLog = "User ${currentUser.email} attempted to respond to invite ${refCode} belonging to ${ci.inviteeEmail}."
            msgUser = "You are not authorised to respond to this invitation — it was sent to a different email address."
        } else {
            if ("accept" == op) {
                retMap.putAll(contributorService.processAccept(ci))
            } else if ("reject" == op) {
                retMap.putAll(contributorService.processReject(ci))
            } else {
                msgUser = "Invalid response. Please use the Accept or Decline buttons."
            }
        }
        retMap.put("confirmed", true)
        if (!retMap.containsKey("msgLog")) retMap.put("msgLog", msgLog)
        if (!retMap.containsKey("msgUser")) retMap.put("msgUser", msgUser)
        if (retMap.get("msgLog")) LOGGER.debug(retMap.get("msgLog") as String)
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
        String refCode = "${String.valueOf(random.nextInt())} ${params["inviterUsername"]?.decodeHTML()}"
        refCode = refCode.encodeAsMD5()
        result.putAll([role: role, refCode: refCode, serverURL: serverURL] as Map)

        String subjectLine = "${inviterName} has invited you to contribute to BioModels as a ${roleName}"
        String howtoAction = "Send"
        // 1. Create a record in the contribution_invite table
        User inviter = User.findByUsername(inviterUsername)
        CI ci = CI.findWhere(inviter: inviter, inviteeEmail: inviteeEmail, revision: revision, role: role)
        Map r = contributorService.findOrCreateInvite(ci, inviterName, inviter, inviteeEmail,
                                    howtoAction, refCode, role, revision)
        result.putAll(r)

        // 2. Send an email having instructions to the invited contributor
        String acceptURL = g.createLink(controller: "contributor", action: "handleInviteResponse",
                params: [ref: refCode, op: 'accept'], absolute: true) as String
        String rejectURL = g.createLink(controller: "contributor", action: "handleInviteResponse",
                params: [ref: refCode, op: 'reject'], absolute: true) as String
        String htmlBody = """
<div style="background-color:#f4f4f4;margin:0;padding:32px 0;font-family:Arial,Helvetica,sans-serif;color:#333333;">
  <div style="max-width:620px;margin:0 auto;background-color:#ffffff;border-radius:4px;overflow:hidden;box-shadow:0 2px 6px rgba(0,0,0,0.10);">
    <div style="background-color:#ED6B21;height:5px;"></div>
    <div style="background-color:#072C55;padding:24px 32px 20px;">
      <div style="font-size:22px;font-weight:bold;color:#ffffff;letter-spacing:0.5px;">BioModels</div>
      <div style="font-size:12px;color:rgba(255,255,255,0.75);margin-top:4px;letter-spacing:0.3px;">Laboratory for Systems Medicine &bull; University of Florida</div>
    </div>
    <div style="padding:32px;font-size:15px;line-height:1.7;color:#333333;">
      <p style="margin:0 0 16px;">${inviterName} has invited you to contribute to a BioModels submission as a <strong>${roleName}</strong>.</p>
      <p style="margin:0 0 16px;">Please indicate whether you would like to accept or decline this invitation:</p>
      <p style="margin:0 0 24px;">
        <a href="${acceptURL}" style="display:inline-block;background-color:#007c82;color:#ffffff;padding:10px 24px;border-radius:4px;text-decoration:none;font-weight:bold;margin-right:12px;">Accept</a>
        <a href="${rejectURL}" style="display:inline-block;background-color:#cccccc;color:#333333;padding:10px 24px;border-radius:4px;text-decoration:none;font-weight:bold;">Decline</a>
      </p>
      <p style="margin:0 0 8px;font-size:13px;color:#666666;">Buttons not working? Copy and paste the links below into your browser:</p>
      <p style="margin:0 0 4px;font-size:13px;">Accept: <a href="${acceptURL}" style="color:#0F5CB1;">${acceptURL}</a></p>
      <p style="margin:0 0 16px;font-size:13px;">Decline: <a href="${rejectURL}" style="color:#0F5CB1;">${rejectURL}</a></p>
      <p style="margin:0 0 16px;">If you do not yet have a BioModels account, please <a href="${serverURL}/registration" style="color:#0F5CB1;">register</a> before accepting.</p>
      <p style="margin:0 0 16px;">Kind regards,<br/><strong>The BioModels Team</strong><br/>
        <a href="${serverURL}" style="color:#0F5CB1;">${serverURL}</a>
      </p>
    </div>
    <hr style="border:none;border-top:1px solid #e8e8e8;margin:0;"/>
    <div style="background-color:#f8f8f8;padding:20px 32px;font-size:12px;color:#777777;line-height:1.6;">
      You are receiving this email because ${inviterName} has invited you to contribute to
      <a href="${serverURL}" style="color:#0F5CB1;">BioModels</a>.
      This is an automatically generated email &mdash; replies are not monitored.
    </div>
  </div>
</div>"""
        if (!mailingService) {
            LOGGER.error("mailingService is null — skipping invitation email to ${inviteeEmail}")
        } else {
            try {
                LOGGER.info("Sending invitation email to ${inviteeEmail}")
                mailingService.send([to: inviteeEmail, subject: subjectLine, html: htmlBody, replyTo: inviterEmail])
                LOGGER.info("Invitation email sent successfully to ${inviteeEmail}")
            } catch (Exception e) {
                LOGGER.error("Failed to send invitation email to ${inviteeEmail}: ${e.message}")
            }
        }

        render(result as JSON)
    }

    @Secured(['ROLE_ADMIN'])
    def load() {
        try {
            Map mapResult
            String modelId = params.id as String
            String revisionId = params.revisionId as String
            Model model = modelService.getModel(modelId)
            mapResult = ContributorService.getContributorsForModel(model, revisionId)
            handleRestApi(mapResult)
        } catch (Exception err) {
            LOGGER.error err.message, err
            forward controller: 'errors', action: 'error404'
        }
    }

    def updateRole() {
        Map result = [:]
        String message = "Under construction"

        String modelId = params.get("modelId")
        Integer revisionNumber = params.getInt("revisionNumber")
        Model model = modelService.getModel("$modelId.$revisionNumber")
        Revision revision = Revision.findByModelAndRevisionNumber(model, revisionNumber)

        String newRoleName = params.get("newRole").decodeHTML()
        CR newRole = CR.findByName(newRoleName)
        String displayName = params.get("displayName").decodeHTML()
        String usernameAndEmail = params.get("usernameAndEmail").decodeHTML()
        usernameAndEmail = usernameAndEmail.replaceAll("\r\n","").trim()
        if (!usernameAndEmail) {
            throw new Exception("Failed to update contributor role because of the empty input.")
        }
        String[] arrayOfStrings = usernameAndEmail.split(", ")
        List parts = arrayOfStrings as List
        if (parts.size() == 1) {
            // go to save the data in the CDWI table
            message = contributorService.updateRoleForExternalContributor(revision, newRole, displayName, parts[0], "")
        } else {
            if (FileHelper.isValidOrcid(parts[0])) {
                // go to save the data in the CDWI table
                message = contributorService.updateRoleForExternalContributor(revision, newRole, displayName, parts[1], parts[0])
            } else {
                String username = parts[0]
                String email = parts[1]
                User contributor = User.findByUsernameAndEmail(username, email)

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
            }
        }

        result.put("message", message)
        render(result as JSON)
    }

    def remove() {
        Map result = [:]
        String message = "In progress"
        Map parsedParameters = parseParameters()
        result.putAll(parsedParameters)
        Boolean external = result.get("external") as Boolean
        if (external) {
            message = contributorService.removeExternalContributor(parsedParameters)
        } else {
            String revisionIdentifier = parsedParameters["revisionIdentifier"]
            String modelId = parsedParameters["modelId"] as String
            User contributor = parsedParameters["contributor"] as User
            Revision revision = parsedParameters["revision"] as Revision
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
                    sendRemovalEmail(contributor, modelId, revisionIdentifier)
                }
                LOGGER.debug(message)
            } catch (OptimisticLockingFailureException exception) {
                message = """\
An error happened when removing the contributor \
${contributor.person.userRealName} (${contributor.username}, ${contributor.email}) \
from the model ${revisionIdentifier}."""
                LOGGER.error(message, exception)
                println "$message: ${exception.toString()}"
                throw exception
            }
        }

        result.put("message", message)
        render(result as JSON)
    }

    private void sendRemovalEmail(User contributor, String modelId, String revisionIdentifier) {
        if (!mailingService) {
            LOGGER.error("mailingService is null — skipping removal notification email to ${contributor.email}")
            return
        }
        def remover = userService.currentUser
        String removerName = remover?.person?.userRealName ?: "A BioModels curator"
        String recipientName = contributor.person?.userRealName ?: contributor.username
        String modelLink = "${serverURL}/${modelId}"
        String subjectLine = "Your contributor access to ${modelId} on BioModels has been removed"
        String htmlBody = """
<div style="background-color:#f4f4f4;margin:0;padding:32px 0;font-family:Arial,Helvetica,sans-serif;color:#333333;">
  <div style="max-width:620px;margin:0 auto;background-color:#ffffff;border-radius:4px;overflow:hidden;box-shadow:0 2px 6px rgba(0,0,0,0.10);">
    <div style="background-color:#ED6B21;height:5px;"></div>
    <div style="background-color:#072C55;padding:24px 32px 20px;">
      <div style="font-size:22px;font-weight:bold;color:#ffffff;letter-spacing:0.5px;">BioModels</div>
      <div style="font-size:12px;color:rgba(255,255,255,0.75);margin-top:4px;letter-spacing:0.3px;">Laboratory for Systems Medicine &bull; University of Florida</div>
    </div>
    <div style="padding:32px;font-size:15px;line-height:1.7;color:#333333;">
      <p style="margin:0 0 16px;">Dear ${recipientName},</p>
      <p style="margin:0 0 16px;">This email is to inform you that ${removerName} has removed you as a contributor from the following BioModels submission:</p>
      <p style="margin:0 0 16px;"><a href="${modelLink}" style="color:#0F5CB1;">${revisionIdentifier}</a></p>
      <p style="margin:0 0 16px;">You will no longer have contributor access to this model. If you believe this was done in error, please contact the BioModels team.</p>
      <p style="margin:0 0 16px;">Kind regards,<br/><strong>The BioModels Team</strong><br/>
        <a href="${serverURL}" style="color:#0F5CB1;">${serverURL}</a>
      </p>
    </div>
    <hr style="border:none;border-top:1px solid #e8e8e8;margin:0;"/>
    <div style="background-color:#f8f8f8;padding:20px 32px;font-size:12px;color:#777777;line-height:1.6;">
      You are receiving this email because your contributor access on
      <a href="${serverURL}" style="color:#0F5CB1;">BioModels</a> has changed.
      This is an automatically generated email &mdash; replies are not monitored.
    </div>
  </div>
</div>"""
        try {
            LOGGER.info("Sending contributor removal notification email to ${contributor.email}")
            mailingService.send([to: contributor.email, subject: subjectLine, html: htmlBody])
            LOGGER.info("Contributor removal notification email sent successfully to ${contributor.email}")
        } catch (Exception e) {
            LOGGER.error("Failed to send contributor removal notification email to ${contributor.email}: ${e.message}")
        }
    }

    private Map parseParameters() {
        User contributor = null
        boolean externalContributor = params.getBoolean("externalContributor")
        String email = ""
        String orcid = ""
        if (params.containsKey("usernameAndEmail")) {
            String usernameAndEmail = params.get("usernameAndEmail").decodeHTML()
            String[] parts = usernameAndEmail.split(", ")
            List list = parts as List
            if (list.size() == 2) {
                String username = list[0]
                email = list[1]
                if (FileHelper.isValidOrcid(username)) {
                    orcid = username
                } else {
                    contributor = User.findByUsernameAndEmail(username, email)
                }
            } else {
                email = list[0]
            }
        }
        String displayName = params.get("userRealName").decodeHTML()
        String modelId = params.get("modelId").decodeHTML()
        Integer revisionNumber = params.getInt("revisionNumber")
        Model model = modelService.getModel("$modelId.$revisionNumber")
        Revision revision = Revision.findByModelAndRevisionNumber(model, revisionNumber)

        String roleName = params.get("roleName").decodeHTML()
        CR role = CR.findByName(roleName)

        [contributor: contributor, revision: revision, modelId: modelId, role: role,
         external: externalContributor, email: email, orcid: orcid, displayName: displayName,
         revisionNumber: revisionNumber, revisionIdentifier: "$modelId.$revisionNumber"]
    }
}
