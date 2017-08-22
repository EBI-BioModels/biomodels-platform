/**
 * Copyright (C) 2010-2016 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

package net.biomodels.jummp.deployment.biomodels

import net.biomodels.jummp.core.model.FlagTransportCommand
import net.biomodels.jummp.core.model.ModelTransportCommand

import java.text.SimpleDateFormat

/**
 * @short General purpose helper for rendering BioModels pages.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
class BioModelsTagLib {
    static defaultEncodeAs = [taglib:'none']
    static namespace = 'biomd'

    /**
     * Declare dependency injections
     */
    def decorationService
    def modelOfTheMonthService
    def modelDelegateService
    /**
     * Displays the Model of the Month (MoM) entry for the given model.
     *
     * @attr modelId REQUIRED the id of the model for which to render
     * the MoM entry.
     */
    def renderModelOfMonth = { attrs ->
        Long id = attrs.modelId
        if (!id) {
            return
        }
        def entries = modelOfTheMonthService.fetchEntriesForModel id
        if (entries) {
            out << "<span>View the Model of the Month entry for this model: </span>"
        }
        out << render(collection: entries, template: '/templates/modelOfTheMonth',
                plugin: 'jummp-plugin-biomodels-dom')
        // calls momService.fetchEntriesForModel for given modelId
        // delegates rendering to dedicated template
    }

    def renderModelFlags = { attrs ->
        def base64Flags = attrs.flags?.collect { FlagTransportCommand cmd ->
            [
                img: Base64.encoder.encodeToString(cmd.icon),
                label: cmd.label,
                description: cmd.description
            ]
        }
        out << render(collection: base64Flags, template: '/templates/modelFlags',
                plugin: 'jummp-plugin-biomodels-dom', var: "flag")
    }

    def renderCurationNotesTab = { attrs ->
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm:ss");
        def base64CurationNotes = attrs.curationNotes?.collect { CurationNotesTransportCommand cmd ->
            [
                model: cmd.model,
                submitter: cmd.submitter,
                lastModifier: cmd.lastModifier,
                dateAdded: dateFormat.format(cmd.dateAdded),
                lastModified: dateFormat.format(cmd.lastModified),
                comment: cmd.comment ?: "",
                curationImage: cmd.curationImage ? Base64.encoder.encodeToString(cmd.curationImage) : null
            ]
        }
        // use class 'row' specifically designed by EBI Visual Framework to gain responsive design performance
        out << "<div id='Curation' class='row'>"
        out << render(collection: base64CurationNotes, template: '/templates/curationNotes',
                    plugin: 'jummp-plugin-biomodels-dom', var: 'curaRec')
        out << "</div>"
    }

    def renderCurationStatus = { attrs ->
        out << attrs.curationStatus
    }

    def renderModellingApproaches = { attrs ->
        out << render(collection:  attrs.modellingApproaches,
            template: '/templates/modellingApproach',
            plugin: 'jummp-plugin-biomodels-dom', var: 'modellingApproach')
    }

    def renderOriginalModels = { attrs ->
        out << render(collection: attrs.sources,
            template: '/templates/originalModel',
            plugin: 'jummp-plugin-biomodels-dom', var: 'source')
    }

    def renderRecentlyAccessedModels = {
        Map<ModelTransportCommand, ModelHits> models = decorationService.getRecentlyAccessedModels()
        StringBuilder result = new StringBuilder("<ul style='list-style: none; " +
            "list-style-position: inside; padding: 0; margin-left: 0'>")
        models?.each {
            ModelTransportCommand mtc = it.key
	        String modelId = mtc.publicationId ?: mtc.submissionId
            String modelURI = g.createLink(controller: 'model', id: modelId, action: 'show')
	        String modelLink = "<li style='text-indent: -1.2em; padding-left: 1em'>" +
                "<span class='icon icon-functional' data-icon='4'>&nbsp;</span>" +
                "<a href='${modelURI}'>${it.value.modelName}</a></li>"
            result.append(modelLink)
        }
	    result.append("</ul>")
        out << result.toString()
    }

    def renderRecentlyPublishedModels = {
        Map<ModelTransportCommand, ModelLatestPublished> models = decorationService.getRecentlyPublishedModels()
        StringBuilder result = new StringBuilder("<ul style='list-style: none; " +
            "list-style-position: inside; padding: 0; margin-left: 0'>")
        models?.each {
            ModelTransportCommand mtc = it.key
	        String modelId = mtc.publicationId ?: mtc.submissionId
            String modelURI = g.createLink(controller: 'model', id: modelId, action: 'show')
            String modelLink= "<li style='text-indent: -1.2em; padding-left: 1em'>" +
                "<span class='icon icon-functional' data-icon='U'>&nbsp;</span>" +
                "<a href='${modelURI}'>${it.value.modelName}</a></li>"
            result.append(modelLink)
        }
	    result.append("</ul>")
        out << result.toString()
    }
}
