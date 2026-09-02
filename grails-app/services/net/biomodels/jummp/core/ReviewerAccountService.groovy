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
 *
 * Additional permission under GNU Affero GPL version 3 section 7
 *
 * If you modify Jummp, or any covered work, by linking or combining it with
 * Apache Commons, Perf4j (or a modified version of that library), containing parts
 * covered by the terms of Apache License v2.0, the licensors of this
 * Program grant you additional permission to convey the resulting work.
 *{Corresponding Source for a non-source form of such a combination shall
 * include the source code for the parts of Apache Commons, Perf4j used as well as
 * that of the covered work.}
 **/

package net.biomodels.jummp.core

import grails.plugin.springsecurity.acl.AclSid
import grails.util.Holders
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.plugins.security.Person
import net.biomodels.jummp.plugins.security.Role
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.plugins.security.UserRole
import net.biomodels.jummp.utils.MathUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean

/**
 * Simple script to create a reviewer account for a set of models.
 *
 * Expected to be used as a Grails console script by an administrator,
 * but the logic could be invoked by a model owner directly from the
 * UI in the future.
 *
 * @author Mihai Glont <mglont@pm.me>
 * @author Tung Nguyen <nvntung@gmail.com>
 * @date 20180626
 */

class ReviewerAccountService extends UserService implements InitializingBean {
    static private final Logger LOGGER = LoggerFactory.getLogger(ReviewerAccountService.class)
    def ms = Holders.grailsApplication.mainContext.modelService
    def sss = Holders.grailsApplication.mainContext.springSecurityService

    User createReviewerUser(final String name, final String password) {
        Person p = new Person(userRealName: name).save()
        User result = new User(
            username: name,
            password: sss.encodePassword(password),
            person: p,
            email: "${name}@reviewers.biomodels.net",
            enabled: true, accountExpired: false, accountLocked: false, passwordExpired: false
        )
        result.save(flush: true)
        new AclSid(sid: name, principal: true).save(flush: true)
        UserRole.create(result, Role.findByAuthority('ROLE_USER'), true)
        UserRole.create(result, Role.findByAuthority('ROLE_REVIEWER'), true)
        result
    }

    ReviewerAccountInfo createReviewerAccount(final String commaSeparatedModels) {
        List<String> modelIDs = parseCommaSeparatedModelIdList(commaSeparatedModels)
        if (!modelIDs) {
            throw new IllegalArgumentException("Please specify which models to create a reviewer account for.")
        }
        def models = Model.findAllBySubmissionIdInList(modelIDs)
        if (models.size() != modelIDs.size()) {
            throw new IllegalArgumentException("One or more of the submission identifiers $modelIDs could not be found")
        }
        for (Model m: models) {
            if (!ms.canAddRevision(m)) { // TODO ensure that this only applies to previously-unpublished models
                throw new IllegalArgumentException(
                    "You can only create a reviewer account for your own models -- model ${m.submissionId} is not one of them")
            }
        }
        // TODO ensure there is no other reviewer account for these models?
        String accountName = formatReviewerAccountName(modelIDs)
        User reviewer = User.findByUsername(accountName)
        String password = MathUtils.generatePassword((('A'..'Z')+('0'..'9')).join(), 6)
        if (!reviewer) {
            reviewer = createReviewerUser(accountName, password)
            LOGGER.debug("A reviewer account [${reviewer.dump()}] has been created for the model(s): ${commaSeparatedModels}.")
            for (Model m: models) {
                ms.grantReadAccess(m, reviewer)
            }
        } else {
            // update the new password to the reviewer account
            reviewer.password = sss.encodePassword(password)
            reviewer.save(flush: true)
        }

        new ReviewerAccountInfo(sharedModels: models, password: password, user: reviewer)
    }

    String createAccountAndInstructions(final String modelsToReview, final String serverURL) {
        ReviewerAccountInfo reviewerInfo = createReviewerAccount(modelsToReview)
        String u = reviewerInfo.user.username
        String p = reviewerInfo.password
        final String CURATION_EMAIL = grailsApplication.config.jummp.model.curators.mailinglist
        String inner = """
      <p style="margin:0 0 16px;">A reviewer account has been created for your model(s) <strong>${modelsToReview}</strong>. Please forward the following credentials to your reviewer(s).</p>
      <p style="margin:0 0 8px;font-weight:bold;">Access instructions</p>
      <ol style="margin:0 0 16px;padding-left:20px;">
        <li style="margin-bottom:8px;">Visit the BioModels login page: <a href="${serverURL}/login/auth" style="color:#0F5CB1;">${serverURL}/login/auth</a></li>
        <li style="margin-bottom:8px;">Log in with username <strong>${u}</strong> and password <strong>${p}</strong></li>
        <li style="margin-bottom:8px;">Access the model directly at: <a href="${serverURL}/${modelsToReview}" style="color:#0F5CB1;">${serverURL}/${modelsToReview}</a></li>
      </ol>
      <p style="margin:0 0 16px;">If you or your reviewer encounter any problems, please contact us at <a
      href="mailto:${CURATION_EMAIL}" style="color:#0F5CB1;">${CURATION_EMAIL}</a>, quoting the username
<strong>${u}</strong>.</p>
      <p style="margin:0 0 16px;">Kind regards,<br/><strong>The BioModels Team</strong><br/>
        <a href="${serverURL}" style="color:#0F5CB1;">${serverURL}</a>
      </p>"""
        String footerNote = """You are receiving this email because a reviewer account has been created for your submission on
      <a href="${serverURL}" style="color:#0F5CB1;">BioModels</a>.
      This is an automatically generated email &mdash; replies are not monitored."""
        String message = mailingService.wrapHtml(inner, [footerNote: footerNote])
        String emailBody = message
        String emailSubject = "[BioModels] Reviewer account for your model ${modelsToReview}"
        def currentUser = sss.currentUser
        def bccRecipients = [grailsApplication.config.jummp.security.registration.email.adminAddress as String]
        mailingService.send([to: currentUser.email, subject: emailSubject, html: emailBody,
                             bcc: bccRecipients])
        return message
    }

    String formatReviewerAccountName(List<String> modelIDs) {
        final String prefix = "reviewerFor"
        StringBuilder result = new StringBuilder(prefix)
        if (!modelIDs) throw new IllegalArgumentException()

        result.append modelIDs.first()
        if (modelIDs.size() > 1) {
            result.append('-').append(modelIDs.last())
        }
        result.toString()
    }

    private static List<String> parseCommaSeparatedModelIdList(String ids) {
        ids?.split(',')?.collect { it?.trim() }
    }

    @Override
    void afterPropertiesSet() throws Exception {
        LOGGER.info("Finished the bean initialisation")
    }
}

class ReviewerAccountInfo {
    User user
    List<Model> sharedModels
    String password

    String toString() {
        "Reviewer account '${user?.username}' with password '$password' for $sharedModels"
    }
}
