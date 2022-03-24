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
import net.biomodels.jummp.deployment.biomodels.CommonController
import net.biomodels.jummp.model.ContributionDetails as CD
import net.biomodels.jummp.model.ContributionRole as CR
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.Revision
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @short Controller class for interacting with user.
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
@Secured(["isAuthenticated()"])
class ContributorController extends CommonController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContributorController.class)

    static allowedMethods = [update: "POST"]

    def grailsApplication
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

    @Secured(['IS_AUTHENTICATED_FULLY'])
    def sendContributionInvite() {
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
        String serverURL = grailsApplication.config.grails.serverURL
        List<String> roles = CR.getAll().collect {
            it.name
        }.sort()
        String htmlString = g.render(template: "/jummp/showContributor",
            plugin: "jummp-plugin-web-application",
            model: [cont: ctc, serverURL: serverURL, roles: roles])
        result.put("htmlBasedStringForNewContributor", htmlString)
        result.put("message", "The data has been updated successfully!")
        render(result as JSON)
    }
}
