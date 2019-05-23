package net.biomodels.jummp.webapp

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import net.biomodels.jummp.core.InvalidPublicationAuthorsException
import net.biomodels.jummp.core.adapters.PublicationAdapter
import net.biomodels.jummp.model.Publication

import net.biomodels.jummp.core.model.PublicationTransportCommand

@Secured(['IS_AUTHENTICATED_FULLY'])
class PublicationController {
    def publicationService
    def pubMedService

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

    def show(Publication publication) {
        if (!publication) {
            render(view: "error404")
            return false
        }
        PublicationTransportCommand pubCmd = new PublicationAdapter(publication: publication).toCommandObject()
        [publication: pubCmd, authorListContainerSize: 4]
    }

    def refreshPubMedData() {
        PublicationTransportCommand pubTC = pubMedService.fetchPublicationData(params.pubmed)
        pubTC.id = params.long("id")
        render template: "/templates/publication/publicationEditableElements",
            plugin: "jummp-plugin-web-application",
            model: [id: params.id, publication: pubTC, authorListContainerSize: 4, controllerName: "publication", actionName: "show"]
    }

    def save(PublicationTransportCommand pubCmd) {
        Map result = [:]
        String message = ""
        Integer status
        if (pubCmd.validate()) {
            message = "Data binding is valid"
            Publication publication = publicationService.fromCommandObject(pubCmd)
            if (publication) {
                message += "<br/>The data have been saved successfully"
                status = 200
            } else {
                message += "<br/>Failures of saving data"
                status = 500
            }
            result.message = message
            result.status = status
        } else {
            result.message = "There have been problems with data binding:<br/>${pubCmd.errors.allErrors.inspect()}"
            result.status = 500
        }
        render(result as JSON)
    }

    def doAddOrUpdate() {

    }
}
