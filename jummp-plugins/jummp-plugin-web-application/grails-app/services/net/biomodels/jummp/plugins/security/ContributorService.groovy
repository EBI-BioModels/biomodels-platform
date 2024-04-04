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

import grails.transaction.Transactional
import net.biomodels.jummp.core.model.ContributorTransportCommand as CTC
import net.biomodels.jummp.core.model.InviteState
import net.biomodels.jummp.model.ContributionDetails as CD
import net.biomodels.jummp.model.ContributionInvite as CI
import net.biomodels.jummp.model.ContributionRole as CR
import net.biomodels.jummp.model.ContributionRole
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
        String msg = ""
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
                    subjectLine = "${inviterName} is still waiting for you to join your submission in BioModels as a ${role.toLowerCase()}"
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
                    subjectLine = "${inviterName} is still waiting for you to join your submission in BioModels as a ${role.toLowerCase()}"
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
                println ctc.toString()
                result.put(ctc.toString(), cd)
            }
        }
        result
    }

    CD findOrSaveContributionDetails(final Revision revision, final CR role) {
        CD cd = CD.findOrSaveWhere(contributor: revision.owner, revision: revision, role: role)
        if (cd.save(flush: true)) {
            LOGGER.debug("Successfully created the contribution details: ${toStringCD(cd)}")
        } else {
            cd = null
            LOGGER.error("Could not create the contribution details: ${toStringCD(cd)}")
        }
        return cd
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

    @Override
    void afterPropertiesSet() throws Exception {
        LOGGER.info("Finished the bean initialisation")
    }
}
