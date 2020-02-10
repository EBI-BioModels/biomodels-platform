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





package net.biomodels.jummp.plugins.sbml

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import net.biomodels.jummp.core.annotation.QualifierTransportCommand
import net.biomodels.jummp.core.annotation.ResourceReferenceTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
import org.springframework.security.access.AccessDeniedException

/**
 * Controller for handling Model files in the SBML format.
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Chinmay Arankalle <carankalle@ebi.ac.uk>
 */
@Secured("IS_AUTHENTICATED_ANONYMOUSLY")
class SbmlController {
    def modelDelegateService
    def metadataDelegateService
    def sbmlService

    def fetchComponents() {
        def components = sbmlService.extract(params.id, params.int('revisionNumber'), params.int('skip'), params.int('limit'), params.typeName)
        render([components: components] as JSON)
    }

    def show = {
        Map model = flash.genericModel
        RevisionTransportCommand r = model.revision as RevisionTransportCommand
        Map<QualifierTransportCommand, List<ResourceReferenceTransportCommand>> genericAnno =
                metadataDelegateService.fetchGenericAnnotations r
        if (genericAnno) {
            model["genericAnnotations"] = genericAnno
        }
        boolean canCheckConsistency = modelDelegateService.canCheckConsistency(r)
        model["canCheckConsistency"] = canCheckConsistency
        render(view: "/model/sbml/show", model: model)
    }

    @Secured(['IS_AUTHENTICATED_FULLY'])
    def checkConsistency() {
        RevisionTransportCommand rev
        try {
            rev = modelDelegateService.getRevisionFromParams(params.id, params.revisionId)
            List<String> errors = new ArrayList<String>()
            sbmlService.checkConsistency(rev, errors)
            String message = errors.size() > 0 ? ">> with ${errors.size()} error(s):" : ">> no error"
            log.info("checking consistency $message")
            String report = message
            if (errors.size()) {
                report = report.concat("<ul>")
                errors.each {
                    log.error(it)
                    println(it)
                    report = report.concat("<li>").concat(it).concat("</li>")
                }
                report = report.concat("</ul>")
            }
            redirect(controller: "model", action: "showWithMessage",
                id: rev.identifier(),
                params: [flashMessage: "Model has been checked consistency with the report: <br/>".concat(report)])
        } catch(AccessDeniedException e) {
            log.error(e.message, e)
            forward(plugin: "jummp-plugin-web-application", controller: "errors", action: "error403")
        } catch(IllegalArgumentException e) {
            log.error(e.message)
            redirect(controller: "model", action: "showWithMessage",
                id: rev.identifier(),
                params: [
                    flashMessage: """\
There is a problem with this version of the model while trying to check its consistency."""
                ])
        }
    }

    def reactionMetaOverview = {
        RevisionTransportCommand rev = modelDelegateService.getLatestRevision(params.id as Long, false)
        [reaction: sbmlService.getReaction(rev)]
    }

    def compartmentMetaOverview = {
        RevisionTransportCommand rev = modelDelegateService.getLatestRevision(params.id as Long, false)
        [compartment: sbmlService.getCompartment(rev)]
    }

    def parameterMetaOverview = {
        RevisionTransportCommand rev = modelDelegateService.getLatestRevision(params.id as Long, false)
        [parameter: sbmlService.getParameter(rev)]
    }

    def math = {
        RevisionTransportCommand rev = modelDelegateService.getLatestRevision(params.id as Long, false)
        [
            reactions: sbmlService.getReactions(rev),
            rules: sbmlService.getRules(rev),
            functions: sbmlService.getFunctionDefinitions(rev),
            events: sbmlService.getEvents(rev)
        ]
    }

    def entity = {
        RevisionTransportCommand rev = modelDelegateService.getLatestRevision(params.id as Long, false)
        [compartments: sbmlService.getCompartments(rev)]
    }

    def compartmentMeta = {
        RevisionTransportCommand rev = modelDelegateService.getLatestRevision(params.id as Long, false)
        [compartment: sbmlService.getCompartment(rev)]
    }

    def speciesMeta = {
        RevisionTransportCommand rev = modelDelegateService.getLatestRevision(params.id as Long, false)
        [species: sbmlService.getSpecies(rev)]
    }

    def parameterMeta = {
        RevisionTransportCommand rev = modelDelegateService.getLatestRevision(params.id as Long, false)
        [parameter: sbmlService.getParameter(rev)]
    }

    def parameter = {
        RevisionTransportCommand rev = modelDelegateService.getLatestRevision(params.id as Long, false)
        [parameters: sbmlService.getParameters(rev), reactionParameters: sbmlService.getLocalParameters(rev)]
    }
}
