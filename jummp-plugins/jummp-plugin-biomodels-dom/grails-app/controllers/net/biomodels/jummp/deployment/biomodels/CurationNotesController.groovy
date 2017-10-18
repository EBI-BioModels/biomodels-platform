package net.biomodels.jummp.deployment.biomodels

import grails.plugin.springsecurity.annotation.Secured

/**
 * @short Service responsible for retrieving CurationNotes entries.
 *
 * This class  is used for dealing with CurationNotes records.
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
@Secured(['ROLE_CURATOR'])
class CurationNotesController {

    def index() {

    }

    def edit() {
        render(view: "edit")
    }

    private update() {

    }
}
