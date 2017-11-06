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

    def index() {

    }

    def edit() {
        render(view: "edit")
    }

    def updateCurationImage() {
        curationNotesService.updateCurationImage(params.model, params.curaImg)
        render "Curation image has been updated successfully"
    }

    def update() {
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
        boolean status = curationNotesService.updateCurationNotes(command)
        if (status) {
            render "Simulation results have been updated successfully"
        } else {
            render "There is an error while it tries to save the curation notes"
        }
    }

    def reset() {

    }
}
