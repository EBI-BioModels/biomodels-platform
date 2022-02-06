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

class ReviewerAccountService extends UserService {
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
            if(!ms.canAddRevision(m)) { // TODO ensure that this only applies to previously-unpublished models
                throw new IllegalArgumentException(
                    "You can only create a reviewer account for your own models -- model ${m.submissionId} is not one of them")
            }
        }
        // TODO ensure there is no other reviewer account for these models?
        String accountName = formatReviewerAccountName(modelIDs)
        String password = MathUtils.generatePassword((('A'..'Z')+('0'..'9')).join(), 6)
        User reviewer = createReviewerUser(accountName, password)
        for (Model m: models) {
            ms.grantReadAccess(m, reviewer)
        }

        new ReviewerAccountInfo(sharedModels: models, password: password, user: reviewer)
    }

    String createAccountAndInstructions(final String modelsToReview, final String serverURL) {
        ReviewerAccountInfo reviewerInfo = createReviewerAccount(modelsToReview)
        String u = reviewerInfo.user.username
        String p = reviewerInfo.password
        return """<p>Please forward the following instructions to the reviewers:</p>

<p>To access these models:</p>
<p>
1. Please visit <a href='${serverURL}/login/auth' target='_blank'>
${serverURL}/login/auth</a><br/>
2. Log in with username <strong>$u</strong> and password <strong>$p</strong><br/>
3. Access the model at this link <a href='${serverURL}/${modelsToReview}' target='_blank'>
${serverURL}/${modelsToReview}</a></p>

<p>In case of problems, please email <em>biomodels-net-support@lists.sf.net</em>, indicating the username <strong>$u</strong>.</p>
"""
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

    private List<String> parseCommaSeparatedModelIdList(String ids) {
        ids?.split(',')?.collect { it?.trim() }
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
