package net.biomodels.jummp.webapp

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import net.biomodels.jummp.core.adapters.PublicationAdapter
import net.biomodels.jummp.core.model.ModelTransportCommand
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

    /**
     * Fetches publication details from PubMed Server, then renders the publication form with these details
     *
     * This action contributes to fetching publication details via identifier from EuropePMC.
     * The identifier can be an PubMed ID or DOI. As of writing these comments, we have implemented DoiService and
     * PubMedService separately because we haven't been aware of the existence of DOI support from the service
     * provider.
     *
     * Our implementation of {@link DoiService} is based on the output of the curl command hitting to https://doi.org
     * directly. The approach works well but does not include the abstract and affiliation.
     *
     * TODO: split the action into two smaller ones: fetch and render
     *
     * @return HTML codes to display in the publication add and edit view
     */
    def fetchPublicationFromPubMedAndRenderPublicationForm() {
        Map data = doVerifyPubLinkAndFetchData()
        String operation = params.get("operation")

        if (data["comesFromDB"] && operation == "add") { // adding
            data["message"] = "The publication exists!"
            data["status"] = "Failed"
            render(data as JSON)
            return
        }
        if (data["status"] != "Failed")  {
            boolean ID_EXISTS = params.containsKey("id") // editing
            if (ID_EXISTS) {
                data["publication"].id = params.long("id")
            }
            List linkSourceTypes = PLP.LinkType.values().collect { it.label }
            render(template: "/templates/publication/publicationDetailForm",
                plugin: "jummp-plugin-web-application",
                model: [id: params.id, publication: data["publication"], comesFromDB: data["comesFromDB"],
                        authorListContainerSize: 4, linkSourceTypes: linkSourceTypes,
                        controller: "publication", operation: operation, url: request.forwardURI])
        } else {
            render(data as JSON)
        }
    }

    def save(PublicationTransportCommand pubCmd) {
        Map result = [:]
        String message = ""
        Integer status
        if (pubCmd.validate()) {
            Publication publication = publicationService.fromCommandObject(pubCmd)
            if (publication) {
                message += "The publication details have been saved successfully"
                status = 200
                result["publicationId"] = publication.id
            } else {
                message += "There have been errors while saving the publication details"
                status = 500
            }
            result.message = message
            result.status = status
        } else {
            // TODO: extract friendly error messages from Errors object
            result.message = "There have been problems with data binding:<br/>${pubCmd.errors.allErrors.inspect()}"
            result.status = 500
        }
        render(result as JSON)
    }

    /**
     * This action is used when hitting on the Update button in the step of providing the publication
     * in the submission or update flow
     *
     * @return A map of initialised and repopulated variables
     */
    def verifyPubLinkAndFetchData() {
        Map data = doVerifyPubLinkAndFetchData()
        render(data as JSON)
    }

    private Map doVerifyPubLinkAndFetchData() {
        PublicationTransportCommand cmd = new PublicationTransportCommand()
        String pubLinkProvider = params.list("pubLinkProvider")[0]
        String pubLink = params.list("pubLink")[0]
        String message
        String status
        boolean comesFromDB = false
        if (pubLinkProvider == "NoPub" && pubLink) {
            message = "Please select a publication link type."
            status = "Failed"
        } else {
            if (!publicationService.verifyLink(pubLinkProvider, pubLink)) {
                message = "The link is not a valid ${pubLinkProvider}"
                status = "Failed"
            } else {
                message = "The publication details have been updated successfully."
                status = "OK"
                cmd = publicationService.createPTCWithMinimalInformation(pubLinkProvider, pubLink, [])
                PDEC ctx = loadOrFetchOrCreatePublication(cmd)
                // reassign cmd to a newly refreshed one
                cmd = ctx?.publication
                if (!cmd?.validate()) {
                    status = "Unavailable"
                    if (cmd?.journal && cmd?.title && cmd?.linkProvider?.linkType == "DOI") {
                        message = """The publication details are the best which our system can automatically
fetch from <a href="https://doi.org/${pubLink}" target="_blank">https://doi.org/${pubLink}</a>. Currently they are
missing the affiliation and synopsis. Please verify the form and fill empty fields in manually."""
                        status = "Warning"
                    } else if (!cmd?.synopsis || !cmd?.affiliation) {
                        status = "Warning"
                        message = """The publication details are fetched incompletely. Please check the empty fields and fill them in manually."""
                    } else {
                        message = "No records are available. Please do check again."
                    }
                }
                comesFromDB = ctx?.comesFromDatabase
            }
        }
        ["message": message, "status": status, "publication": cmd, "comesFromDB": comesFromDB]
    }

    def doVerifyPublicationProviderAndLink() {
        PublicationTransportCommand cmd = new PublicationTransportCommand()
        String pubLinkProvider = params.list("pubLinkProvider")[0]
        String pubLink = params.list("pubLink")[0]
        String message
        String status
        if (pubLinkProvider == "NoPub" && pubLink) {
            message = "Please select a publication link type."
            status = "Failed"
        } else {
            if (!publicationService.verifyLink(pubLinkProvider, pubLink)) {
                message = "The link is not a valid ${pubLinkProvider}"
                status = "Failed"
            } else {
                message = "The publication details have been updated successfully."
                status = "OK"
            }
        }
        render(["message": message, "status": status] as JSON)
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

    private PDEC loadOrFetchOrCreatePublication(PublicationTransportCommand pubTC) {
        try {
            PDEC publicationContext = publicationService.getPublicationExtractionContext(pubTC)
            if (publicationContext.publication) {
                if (publicationContext.comesFromDatabase) {
                    flash.flashMessage = g.message(code: "publication.editor.duplicateEntry.message")
                }
            } else {
                PublicationTransportCommand retrieved
                retrieved = publicationService.createPTCWithMinimalInformation(params.PubLinkProvider, params.PublicationLink, [])
                publicationContext.publication = retrieved
                publicationContext.comesFromDatabase = false
            }
            return publicationContext
        } catch (Exception e) {
            log.error(e.message, e)
            return null
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
