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
    def userService

    def index() {

    }

    private sanitiseParams() {
        def modelPerennialOrSubmissionId = params.model
        Model model = Model.findByPublicationIdOrSubmissionId(modelPerennialOrSubmissionId, modelPerennialOrSubmissionId)
        CurationNotesTransportCommand curationNotesTC = curationNotesService.fetchCurationNotesForModel(model.id)
        if (!curationNotesTC) {
            // this case is the adding a new curation notes
            curationNotesTC = new CurationNotesTransportCommand()
            curationNotesTC.id = -1
            curationNotesTC.comment = null
            curationNotesTC.dateAdded = new Date()
            curationNotesTC.lastModified = new Date()
            curationNotesTC.curationImage = null
            curationNotesTC.submitter = userService.getCurrentUser()
            curationNotesTC.lastModifier = curationNotesTC.submitter
        } else {
            curationNotesTC.lastModifier = userService.getCurrentUser()
            curationNotesTC.lastModified = new Date()
        }
        String curationImage
        curationImage = curationNotesTC.curationImage ? Base64.encoder.encodeToString(curationNotesTC.curationImage) : null
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        ['curationNotesTC': curationNotesTC, 'curationImage': curationImage,
         'dateFormat': dateFormat, 'id': modelPerennialOrSubmissionId]
    }

    private parseCuratioNotes() {
        def curationNotes = new JsonSlurper().parseText(params.curationNotes)
        Long id = curationNotes["id"]
        String modelId = params.model
        String comment = curationNotes["comment"]
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
        def bindingMap = [id: id,
                          model: modeltc,
                          comment: comment,
                          submitter: submitter,
                          lastModifier: lastModifier,
                          dateAdded: dateAdded,
                          lastModified: lastModified]
        CurationNotesTransportCommand command = new CurationNotesTransportCommand(bindingMap)
        if (curationNotes["curationImage"]) {
            command.curationImage = Base64.decoder.decode(curationNotes["curationImage"])
        }
        command
    }

    def edit() {
        def data = sanitiseParams()
        render(view: "edit", model: data)
    }

	def add() {
        def data = sanitiseParams()
		render(view: "add", model: data)
	}

    def doAddOrUpdate() {
        CurationNotesTransportCommand command = parseCuratioNotes()
        // get the latest timestamp
        command.lastModified = new Date()
        if (command.id < 0) {
            command.dateAdded = command.lastModified
        }
        boolean status = curationNotesService.doAddOrUpdateCurationNotes(command)
        String message
        if (status) {
            message = "Curation notes have been updated successfully"
            render message
            log.debug(message)
        } else {
            message = "There is an error while trying to persist the curation notes into the database"
            render message
            log.error(message)
        }
    }

    def reset() {

    }
}
