package net.biomodels.jummp.webapp

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import net.biomodels.jummp.core.adapters.PublicationAdapter
import net.biomodels.jummp.model.Publication
import net.biomodels.jummp.core.model.PublicationTransportCommand
import net.biomodels.jummp.model.PublicationLinkProvider as PLP
import net.biomodels.jummp.core.model.PublicationDetailExtractionContext as PDEC
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
        [publication: pubTC, title: "A a new publication | BioModels", style: style, serverUrl: serverUrl,
         controller: "publication", operation: "add"]
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
         style: style, serverUrl: serverUrl, controller: "publication", operation: "edit"]
    }

    def refreshPubMedData() {
        PublicationTransportCommand pubTC = pubMedService.fetchPublicationData(params.pubmed)
        pubTC.id = params.long("id")
        List linkSourceTypes = PLP.LinkType.values().collect { it.label }
        String operation = params.get("operation")
        render template: "/templates/publication/publicationDetailForm",
            plugin: "jummp-plugin-web-application",
            model: [id: params.id, publication: pubTC, authorListContainerSize: 4, linkSourceTypes: linkSourceTypes,
                    controller: "publication", operation: operation, url: request.forwardURI]
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

    def doVerifyPubLinkAndFetchData() {
        PublicationTransportCommand cmd = new PublicationTransportCommand()
        String pubLinkProvider = params.list("pubLinkProvider")[0]
        String pubLink = params.list("pubLink")[0]
        String message
        String status
        PDEC ctx = new PDEC()
        if (pubLinkProvider == "NoPub" && pubLink) {
            message = "Please select a publication link type."
            status: "Failed"
        }
        if (!publicationService.verifyLink(pubLinkProvider, pubLink)) {
            message = "The link is not a valid ${pubLinkProvider}"
            status = "Failed"
        } else {
            message = "The publication details have been updated successfully."
            status = "OK"
            cmd = publicationService.fetchPublicationData(pubLinkProvider, pubLink)

            if (!cmd.validate()) {
                status = "Unavailable"
                if (cmd.journal && cmd.title && cmd.linkProvider.linkType == "DOI") {
                    message = """The publication details are the best which our system can automatically
fetch from <a href="https://doi.org/${pubLink}" target="_blank">https://doi.org/${pubLink}</a>. Currently they are
missing the affiliation and synopsis. Please verify the form and fill empty fields in manually."""
                    status = "Warning"
                } else {
                    message = "No records are available. Please do check again."
                }
            } else {
                ctx = publicationService.getPublicationExtractionContext(cmd)
            }
        }
        render(["message": message, "status": status, "publication": cmd,
                "comesFromDB": ctx?.comesFromDatabase] as JSON)
    }

    def validatePublicationDetails() {
        Map result = publicationService.buildPublicationFromJSONData(params.pubDetails.decodeHTML())
        render(result as JSON)
    }


    def renderPublicationDetails() {
        if (!params.pubDetails) {
            render("No publication provided")
        } else {
            // this action is often called to display the publication which has been validated
            // so we don't need to handle exception
            PublicationTransportCommand tempPTC = new PublicationTransportCommand()//pubContext.publication
            def pubDetails = JSON.parse(params.pubDetails.decodeHTML())
            bindData(tempPTC, pubDetails, [exclude: ['authors']])
            publicationService.assembleAuthors(tempPTC, pubDetails.authors)
            render(template: "/templates/showPublication", model: [publication: tempPTC, isUpdate: false])
        }
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
