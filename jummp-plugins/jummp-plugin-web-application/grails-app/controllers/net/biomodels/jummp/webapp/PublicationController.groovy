package net.biomodels.jummp.webapp

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import net.biomodels.jummp.core.InvalidPublicationAuthorsException
import net.biomodels.jummp.model.Publication

import net.biomodels.jummp.core.model.PublicationTransportCommand

@Secured(['IS_AUTHENTICATED_FULLY'])
class PublicationController {
    def publicationService

    def index() {
        List<PublicationTransportCommand> publications = new ArrayList<>()
        publications = publicationService.getAll()
        [publications: publications]
    }

    def add(Publication publication) {
        PublicationTransportCommand pubTC = publicationService.createPTCWithMinimalInformation("PubMed ID", null, null)
        pubTC.id = null
        [publication: pubTC]
    }
    /*def show(PublicationTransportCommand pubCmd) {
        if (!pubCmd) {
            // render out the error
        }
        [publication: pubCmd]
    }*/

    def show(Publication publication) {
        if (!publication) {
            render(view: "error404")
            return false
        }
        def pubCmd = publicationService.getById(params.long("id"))
        [publication: pubCmd, authorListContainerSize: pubCmd?.authors?.size() > 5 ? 5 : pubCmd?.authors?.size()]
    }

    def save() {
        Map result = [:]
        Long id = params.long("id")
        println "Id: ${id}"
        result.message = "Saved successfully"
        /*if (id) {
            PublicationTransportCommand tempPTC = new PublicationTransportCommand()
            bindData(tempPTC, params, [exclude: ['authors']])
            try  {
                publicationService.assembleAuthors(tempPTC, params.authorListContainer)
            } catch (InvalidPublicationAuthorsException e) {
                String errMsg = e.getI18nErrorMessage4InvalidAuthor()
                flash.flashMessage = "There have been errors while parsing authors of the publication:<br/>${errMsg}"
            }
            if (tempPTC.hasErrors()) {
                flash.validationErrorOn = tempPTC
            }
        }*/
        render(result as JSON)
    }

    def doAddOrUpdate() {

    }
}
