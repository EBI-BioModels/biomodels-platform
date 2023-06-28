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

import grails.plugin.springsecurity.annotation.Secured
import net.biomodels.jummp.core.annotation.QualifierTransportCommand as QualifierTC
import net.biomodels.jummp.core.annotation.ResourceReferenceTransportCommand as RRTC
import net.biomodels.jummp.core.annotation.StatementTransportCommand as STC
import net.biomodels.jummp.core.model.RevisionTransportCommand as RevisionTC
import org.springframework.security.access.AccessDeniedException

/**
 * Controller for handling Model files in the SBML format.
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
class SbmlController {
    def modelDelegateService
    def metadataDelegateService
    def sbmlService

    def show = {
        Map model = flash.genericModel
        final String perennialId = params.id
        RevisionTC r = model.revision as RevisionTC
        List<STC> statements = model.modelLevelAnnotations as List<STC>
        Map<QualifierTC, List<RRTC>> annotations = metadataDelegateService.fetchGenericAnnotations(statements)
        if (annotations) {
            model["genericAnnotations"] = annotations
        }
        if (!perennialId.startsWith("BMID") && !perennialId.startsWith("MODEL170711")) {
            def components = [:]
            try {
                components = sbmlService.extractComponentsFromBP(perennialId)
            } catch (RuntimeException re) {
                log.error("Error while extracting components from BP for $perennialId", re)
            }
            if (components.get("species") || components.get("reactions")) {
                model['components'] = components
            }
        }
        boolean canCheckConsistency = modelDelegateService.canCheckConsistency(r)
        model["canCheckConsistency"] = canCheckConsistency
        render(view: "/model/sbml/show", model: model)
    }

    @Secured(['IS_AUTHENTICATED_FULLY'])
    def checkConsistency() {
        RevisionTC rev
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
        RevisionTC rev = modelDelegateService.getLatestRevision(params.id as Long, false)
        [reaction: sbmlService.getReaction(rev)]
    }

    def compartmentMetaOverview = {
        RevisionTC rev = modelDelegateService.getLatestRevision(params.id as Long, false)
        [compartment: sbmlService.getCompartment(rev)]
    }

    def parameterMetaOverview = {
        RevisionTC rev = modelDelegateService.getLatestRevision(params.id as Long, false)
        [parameter: sbmlService.getParameter(rev)]
    }

    def math = {
        RevisionTC rev = modelDelegateService.getLatestRevision(params.id as Long, false)
        [
            reactions: sbmlService.getReactions(rev),
            rules: sbmlService.getRules(rev),
            functions: sbmlService.getFunctionDefinitions(rev),
            events: sbmlService.getEvents(rev)
        ]
    }

    def entity = {
        RevisionTC rev = modelDelegateService.getLatestRevision(params.id as Long, false)
        [compartments: sbmlService.getCompartments(rev)]
    }

    def compartmentMeta = {
        RevisionTC rev = modelDelegateService.getLatestRevision(params.id as Long, false)
        [compartment: sbmlService.getCompartment(rev)]
    }

    def speciesMeta = {
        RevisionTC rev = modelDelegateService.getLatestRevision(params.id as Long, false)
        [species: sbmlService.getSpecies(rev)]
    }

    def parameterMeta = {
        RevisionTC rev = modelDelegateService.getLatestRevision(params.id as Long, false)
        [parameter: sbmlService.getParameter(rev)]
    }

    def parameter = {
        RevisionTC rev = modelDelegateService.getLatestRevision(params.id as Long, false)
        [parameters: sbmlService.getParameters(rev), reactionParameters: sbmlService.getLocalParameters(rev)]
    }
}
