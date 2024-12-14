/**
 * Copyright (C) 2010-2024 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

import grails.transaction.Transactional
import net.biomodels.jummp.core.model.ContributorTransportCommand as CTC
import net.biomodels.jummp.core.model.InviteState
import net.biomodels.jummp.model.ContributionDetails as CD
import net.biomodels.jummp.model.ContributionDetailsWithoutInvite as CDWI
import net.biomodels.jummp.model.ContributionInvite as CI
import net.biomodels.jummp.model.ContributionRole as CR
import net.biomodels.jummp.model.ContributionRole
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.Revision
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean

/**
 * A class for handling services connecting to model contributors
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
@Transactional
class ContributorService implements InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContributorService.class)
    def grailsLinkGenerator
    static List<String> roles

    void init() {
        roles = CR.getAll().collect { it.name }.sort { it }
    }

    static User createDummyUserPerson(final String displayName, final String email, final String affiliation = "",
                                      final String orcid = "") {
        User user = new User(email: email, username: email)
        Person person = new Person(userRealName: displayName)
        if (affiliation) { person.institution = affiliation }
        if (orcid) { person.orcid = orcid }
        user.person = person
        user
    }

    Map createFirstContributors(final Revision revision) {
        Map<String, CTC> contributors = new HashMap<>()
        CTC ctc = new CTC(user: revision.owner, role: ContributionRole.findByName("Curator"),
            person: revision.owner.person, locked: true)
        contributors.put(revision.owner.username, ctc)
        contributors
    }

    Map findOrCreateInvite(final CI ci, final String inviterName, final User inviter, final String inviteeEmail,
                           String howtoAction, final String refCode, final CR role, final Revision revision) {
        Map result = [:]
        String msg
        String subjectLine = "${inviterName} invited you to join your submission in BioModels as as a ${role.name.toLowerCase()}"
        String emailHeading = "You are invited!"
        howtoAction = "Send"
        if (ci) {
            switch (ci.state) {
                case InviteState.ACCEPTED:
                    msg = "There has been an user in our system registed with the email ${inviteeEmail}."
                    // do nothing
                    break
                case InviteState.PENDING:
                    msg = "A gentle reminder has been sent to the email ${inviteeEmail}."
                    subjectLine = "${inviterName} is still waiting for you to join your submission in BioModels as the ${role.name}."
                    emailHeading = "In case you missed it..."
                    howtoAction = "Remind"
                    break
                case InviteState.CANCELLED:
                    msg = "An invitation has been sent to the email ${inviteeEmail}."
                    howtoAction = "Resend"
                    break
                case InviteState.REJECTED:
                    msg = "An invitation has been sent to the email ${inviteeEmail}."
                    howtoAction = "Resend"
                    break
                case InviteState.RESENT:
                    msg = "A gentle reminder has been sent to the email ${inviteeEmail}."
                    subjectLine = "${inviterName} is still waiting for you to join your submission in BioModels as the ${role.name}."
                    emailHeading = "In case you missed it..."
                    howtoAction = "Remind"
                    break
                default:
                    msg = "An invitation has been sent to the email ${inviteeEmail}."
                    break
            }
            ci.reference = refCode
            ci.dateSent = new Date()
            if ("Remind" == howtoAction) {
                ci.state = InviteState.RESENT
            } else {
                ci.state = InviteState.PENDING
            }

            if (ci.merge(flush: true)) {
                msg += " The invitation has been updated or the reminder has been sent successfully."
            } else {
                msg += " The invitation has been updated or the reminder has been sent unsuccessfully. The cause is "
                msg += "${ci.errors.toString()}"
            }
        } else {
            howtoAction = "Send"
            CI _ci = new CI(inviter: inviter, inviteeEmail: inviteeEmail, reference: refCode,
                revision: revision, role: role, dateSent: new Date(), state: InviteState.PENDING)
            if (_ci.save(insert: true, flush: true)) {
                msg = "An invitation has been sent to the email ${inviteeEmail}. "
                msg += "Created a contribution invite (user: ${inviteeEmail}) successfully."
                result.put("contribution_invite_id", _ci.id.toString())
            } else {
                msg = "Failed: ${_ci.errors.toString()}"
            }
        }

        result.putAll(["message": msg, "howtoAction": howtoAction,
                       "subjectLine": subjectLine, "emailHeading": emailHeading])
        result
    }

    Map processAccept(final CI ci) {
        String msgLog
        String msgUser
        Map ret = [:]
        if (ci) {
            String inviteeEmail = ci.inviteeEmail
            String modelId = ci.revision.model.submissionId
            User inviteeAccount = User.findByEmail(inviteeEmail)
            ret.put("modelId", modelId)
            ret.put("inviteeEmail", inviteeEmail)
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
                    String modelURL = grailsLinkGenerator.link(controller: "model", action: "show", id: modelId)
                    modelURL = '<a href="' + modelURL + '" target="_blank">' + modelId + '</a>'
                    msgLog = "The user (${inviteeEmail}) as the ${ci.role.name} joined the model ${modelId}."
                    msgUser = "You have accepted the invitation for the contributions to your model ${modelURL}."
                }
            } else {
                msgLog = "Sorry, we couldn't find any user registered with the email ${inviteeEmail}"
                msgUser = "Sorry, we couldn't find any user registered with the email ${inviteeEmail}"
            }
        } else {
            msgLog = "The invitation is invalid due to expired, cancelled or rejected."
            msgUser = "The invitation is invalid due to expired, cancelled or rejected."
        }
        ret.putAll([msgLog: msgLog, msgUser: msgUser] as Map)
        ret
    }

    Map processReject(final CI ci) {
        String msgLog
        String msgUser
        if (ci) {
            msgLog = "Thank you for your response! We are always happy to support you in the near future."
            msgUser = msgLog
            updateCI(ci, "reject")
            // TODO: update the contributor list either remove this user or expire the invitation: accepted and not allow to revert
        } else {
            msgLog = "The invitation is invalid due to expired, cancelled or rejected."
            msgUser = "The invitation is invalid due to expired, cancelled or rejected."
        }
        [msgLog: msgLog, msgUser: msgUser]
    }

    Map<String, CD> saveFirstContributors(final Map<String, CTC> contributors, final Revision revision) {
        Map<String, CD> result = [:]
        for (CTC ctc: contributors.values()) {
            CD cd = CD.findOrSaveWhere(contributor: ctc.user, revision: revision, role: ctc.role)
            if (cd.save(flush: true)) {
                String s = "Succeeded to save the first contributor(s)" + ctc.dump()
                println(s)
                LOGGER.info(s)
                result.put(ctc.toString(), cd)
            }
        }
        result
    }

    CD findOrSaveContributionDetails(final Revision revision, final String roleName) {
        CR role = CR.findByName(roleName)
        findOrSaveContributionDetails(revision, role)
    }

    CD findOrSaveContributionDetails(final Revision revision, final CR role) {
        CD cd = CD.findOrSaveWhere(contributor: revision.owner, revision: revision, role: role)
        if (cd.save(flush: true)) {
            LOGGER.debug("""Successfully created the contribution details: \
${toStringCD(cd)}. Added ${revision.owner.username} as a ${role.name} for the revision ${revision.id} successfully.""")
        } else {
            cd = null
            LOGGER.error("""Could not create the contribution details: ${toStringCD(cd)}. \
Failed to add ${revision.owner.username} as a ${role.name} for the revision ${revision.id}.""")
        }
        return cd
    }

    /**
     * Consolidates the contributors from all revisions
     * @param model {@link Model} instance
     * @return a map showing the relationships between users and models/revisions
     */
    static Map<String, List<CTC>> getContributorsForModel(Model model, final String revisionId = null) {
        Map<String, List<CTC>> mapResult = new HashMap<>()
        // this implementation isn't optimised but is greedy
        Set<Revision> allRevs = model.revisions
        if (revisionId) {
            int revisionNumber = Integer.parseInt(revisionId)
            allRevs = allRevs.findAll {it.revisionNumber <= revisionNumber }
        }
        for (Revision revision: allRevs) {
            Map result = getContributors(revision)
            result.each { String key, List<CTC> value ->
                if (mapResult.containsKey(key)) {
                    mapResult.get(key).addAll(value)
                } else {
                    mapResult.put(key, value)
                }
            }
        }
        return mapResult
    }

    /**
     * Gets the contributors of a given revision
     * @param revision {@link Revision} instance
     * @return a map showing the relationships between users and models/revisions
     */
    static Map<String, List<CTC>> getContributors(Revision revision) {
        Model model = revision.model
        if (!model) { return null }
        Map<String, List<CTC>> contributorMap = [:]
        List revisions = model.revisions.toList().findAll {
            it.revisionNumber <= revision.revisionNumber
        }

        Set authors = revisions*.owner?.collect { it.username }?.toSet()

        List details = CD.findAllByRevision(revision)
        for (CD detail: details) {
            String username = detail.contributor.username
            boolean locked = username in authors
            CTC ctc = new CTC(user: detail.contributor,
                role: detail.role, person: detail.contributor.person, locked: locked, external: false)
            if (contributorMap.containsKey(username)) {
                contributorMap.get(username).add(ctc)
            } else {
                contributorMap.put(username, [ctc] as List<CTC>)
            }
        }
        List lstContWtoInvite = CDWI.findAllByRevision(revision)
        lstContWtoInvite.each {
            User user = createDummyUserPerson(it.displayName, it.email, it.affiliation, it.orcid)
            Person person = user.person
            if (it.affiliation) { person.institution = it.affiliation }
            if (it.orcid) { person.orcid = it.orcid }
            CTC ctc = new CTC(user: user, role: it.role, person: person, locked: false, external: true)
            if (contributorMap.containsKey(user.username)) {
                contributorMap.get(user.username).add(ctc)
            } else {
                contributorMap.put(user.username, [ctc] as List<CTC>)
            }
        }

        contributorMap
    }

    String updateRoleForExternalContributor(final Revision revision, final CR role, final String displayName,
                                            final String email, final String orcid = "") {

        List result = findCDWI(revision.id, displayName, email, orcid)
        CDWI cdwi
        if (result?.size()) {
            cdwi = CDWI.get(result.get(0))
            cdwi.role = role
            if (!cdwi.save(flush: true)) {
                return "An error has happened while trying to update the role for this contributor."
            } else {
                return "Update the new role for this contributor successfully."
            }
        } else {
            cdwi = CDWI.findOrSaveWhere(revision: revision, role: role, displayName: displayName, email: email, orcid: orcid)
            if (!cdwi.id) {
                LOGGER.error("""Cannot create or save the contribution details [revision: ${revision.id}, ${revision.name}, \
role: ${role.name}, email: ${email}, orcid: ${orcid}]""")
                return "An error has happened while trying to update the role for this contributor."

            } else {
                LOGGER.info("""Succeed saving the contribution details [revision: ${revision.id}, ${revision.name}, \
role: ${role.name}, email: ${email}, orcid: ${orcid}]""")
                return "Update the new role for this contributor successfully."
            }
        }
    }

    String removeExternalContributor(final Map parsedParams) {
        String message
        Revision revision = parsedParams.get("revision") as Revision
        String displayName = parsedParams.get("displayName")
        String email = parsedParams.get("email")
        String orcid = parsedParams.get("orcid")
        CR role = parsedParams.get("role") as CR
        List result = findCDWI(revision.id, displayName, email, orcid)
        if (result.size()) {
            CDWI cdwi = CDWI.get(result.get(0))
            Long id = cdwi.id
            cdwi.delete(flush: true)
            cdwi = CDWI.get(id)
            String contInfo = "[revision: ${revision.id}, ${revision.name}; role: ${role.name}; email: ${email}; orcid: ${orcid}]"
            if (cdwi) {
                message = "Failed to remove the external contributor $contInfo."
            } else {
                message = "Succeed to remove the external contributor $contInfo."
            }
            LOGGER.info(message)
        } else {
            message = """An error happened when removing the contributor \
${displayName} (${orcid}, ${email}) from the model ${revision.model.submissionId}."""
            LOGGER.error(message)
        }
        message
    }

    // TODO: move the following method to ContributionDetails domain class
    String toStringCD(final CD cd) {
        "[${cd.contributor.username}\t ${cd.role.name}\t ${cd.revision.id}: ${cd.revision.name}]".toString()
    }

    private static CI updateCI(CI ci, String userResponse) {
        ci.dateCompleted = new Date()
        switch (userResponse) {
            case "accept":
                ci.state = InviteState.ACCEPTED
                break
            case "reject":
                ci.state = InviteState.REJECTED
                break
        }
        ci.merge(flush: true)
        return ci
    }

    private static List findCDWI(final Long revisionId, final String displayName, final String email, final String orcid) {
        Map namedParams = [revId: revisionId, displayName: displayName, email: email]
        String query = """select CD.id from ContributionDetailsWithoutInvite as CD where CD.revision.id = :revId
and CD.displayName = :displayName and CD.email = :email
"""
        if (orcid) {
            query += " and CD.orcid = :orcid"
            namedParams.put("orcid", orcid)
        }
        List result = CDWI.executeQuery(query, namedParams)
        result
    }

    @Override
    void afterPropertiesSet() throws Exception {
        LOGGER.info("Finished the bean initialisation")
    }
}
