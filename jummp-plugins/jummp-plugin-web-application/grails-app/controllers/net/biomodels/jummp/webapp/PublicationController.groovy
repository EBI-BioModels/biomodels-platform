package net.biomodels.jummp.webapp

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import net.biomodels.jummp.core.adapters.PublicationAdapter
import net.biomodels.jummp.model.Publication

import net.biomodels.jummp.core.model.PublicationTransportCommand
import org.codehaus.groovy.grails.plugins.support.aware.GrailsConfigurationAware
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Secured(['IS_AUTHENTICATED_FULLY'])
class PublicationController implements GrailsConfigurationAware {
    private static final Logger logger = LoggerFactory.getLogger(PublicationController.class)
    def publicationService
    def pubMedService
    String style
    String serverUrl

    def index() {
        List<PublicationTransportCommand> publications = new ArrayList<>()
        publications = publicationService.getAll()
        [publications: publications, title: "List of all publications | BioModels", style: style, serverUrl: serverUrl]
    }

    def add(Publication publication) {
        PublicationTransportCommand pubTC = publicationService.createPTCWithMinimalInformation("PubMed ID", null, null)
        pubTC.id = null
        [publication: pubTC, title: "A a new publication | BioModels", style: style, serverUrl: serverUrl]
    }

    def show(Publication publication) {
        if (!publication) {
            showError404()
        }
        PublicationTransportCommand pubCmd = new PublicationAdapter(publication: publication).toCommandObject()
        [publication: pubCmd, title: "Show the publication ${pubCmd.id} | BioModels",
         style: style, serverUrl: serverUrl]
    }

    def edit(Publication publication) {
        if (!publication) {
            logger.error("Publication (with id: ${publication?.id}) cannot be found in the database.")
            showError404()
        }
        PublicationTransportCommand pubCmd = new PublicationAdapter(publication: publication).toCommandObject()
        [publication: pubCmd, authorListContainerSize: 4, title: "Edit the publication ${pubCmd.id} | BioModels",
         style: style, serverUrl: serverUrl]
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
                result["publicationId"] = publication.id
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

    private def showError404() {
        render(view: "error404")
        return false
    }

    @Override
    void setConfiguration(ConfigObject co) {
        style = co.jummp.branding.style
        serverUrl = co.grails.serverURL
    }
}
