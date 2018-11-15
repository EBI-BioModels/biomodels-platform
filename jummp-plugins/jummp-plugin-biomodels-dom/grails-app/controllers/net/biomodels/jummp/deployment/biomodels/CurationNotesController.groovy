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

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import groovy.json.JsonSlurper
import net.biomodels.jummp.core.adapters.ModelAdapter
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.plugins.security.User

import java.text.SimpleDateFormat

/**
 * @short Service responsible for retrieving CurationNotes entries.
 *
 * This class  is used for dealing with CurationNotes records.
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
@Secured(['ROLE_CURATOR'])
class CurationNotesController {
    def curationNotesService

    def index() {

    }

    private parseCuratioNotes(def curationNotes, def modelId) {
        curationNotes = new JsonSlurper().parseText(curationNotes)
        String comment = curationNotes["comment"].encodeAsHTML()
	    String internalComment = curationNotes["internalComment"].encodeAsHTML()
        String submitterUsername = curationNotes["submitter"]
        User submitter = User.findByUsername(submitterUsername)
        String lastModifierUsername = curationNotes["lastModifier"]
        User lastModifier = User.findByUsername(lastModifierUsername)
        def newDateAdded = curationNotes["dateAdded"]
        def newLastModified = curationNotes["lastModified"]
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        Date dateAdded = dateFormat.parse(newDateAdded)
        Date lastModified = dateFormat.parse(newLastModified)
        Model model = Model.findByPublicationIdOrSubmissionId(modelId, modelId)
        ModelTransportCommand modeltc = new ModelAdapter(model: model).toCommandObject()
        boolean updated = curationNotes["updated"]
        def bindingMap = [model: modeltc,
                          comment: comment,
                          internalComment: internalComment,
                          submitter: submitter,
                          lastModifier: lastModifier,
                          dateAdded: dateAdded,
                          lastModified: lastModified,
                          updated: updated]
        CurationNotesTransportCommand command = new CurationNotesTransportCommand(bindingMap)
        if (params?.cnId) {
            command.id = params.long("cnId")
        }
        if (curationNotes["curationImage"]) {
            command.curationImage = Base64.decoder.decode(curationNotes["curationImage"])
            command.mimeType = curationNotes["mimeType"]
        }
        command
    }

    def show() {
        def data = curationNotesService.loadOrInitialise(params)
        render(view: "curationNotesEditor", model: data)
    }

    def doAddOrUpdate() {
        def curationNotes = params.curationNotes
        String model = params.model
        model = model.encodeAsHTML()
        CurationNotesTransportCommand command = parseCuratioNotes(curationNotes, model)
        // get the latest timestamp
        command.lastModified = new Date()
        if (!command.updated) {
            // this case means to add a new curation notes
            command.dateAdded = command.lastModified
        }
        Map response = [:]
        if (command.validate()) {
            CurationNotes update = curationNotesService.doAddOrUpdateCurationNotes(command)
            if (update) {
                response['message'] = "Curation notes have been updated successfully"
                response['cnId'] = update.id
            } else {
                response['message'] = "There is an error while trying to persist the curation notes into the database"
            }
        } else {
            String defaultMessage = command.errors.getFieldError("comment")?.defaultMessage
            if (defaultMessage?.contains("cannot be blank")) {
                response['message'] = "The comment cannot be blank"
            } else {
                response['message'] = command.errors.allErrors.inspect()
            }
        }
        render(response as JSON)
    }

    def reset() {

    }
}
