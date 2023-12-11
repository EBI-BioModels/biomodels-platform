package net.biomodels.jummp.webapp

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import net.biomodels.jummp.core.adapters.PublicationAdapter
import net.biomodels.jummp.core.model.PublicationDetailExtractionContext as PDEC
import net.biomodels.jummp.core.model.PublicationTransportCommand as PubTC
import net.biomodels.jummp.model.Publication
import net.biomodels.jummp.model.PublicationLinkProvider as PLP
import net.biomodels.jummp.utils.CollectionHelper
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
        List<PubTC> publications = publicationService.getAll()
        [publications: publications, title: "List of all publications | BioModels", style: style, serverUrl: serverUrl]
    }

    def add(Publication publication) {
        PubTC pubTC = publicationService.createPTCWithMinimalInformation("PubMed ID", null, null)
        pubTC.id = -1 // assign it a dummy value to avoid exceptions
        [publication: pubTC, title: "A a new publication | BioModels", style: style, serverUrl: serverUrl,
         controller: "publication", operation: "add"]
    }

    def show(Publication publication) {
        if (!publication) {
            showError404()
        }
        PubTC pubCmd = new PublicationAdapter(publication: publication).toCommandObject()
        [publication: pubCmd, title: "Show the publication ${pubCmd.id} | BioModels",
         style: style, serverUrl: serverUrl]
    }

    def edit(Publication publication) {
        if (!publication) {
            logger.error("Publication (with id: ${publication?.id}) cannot be found in the database.")
            showError404()
        }
        PubTC pubCmd = new PublicationAdapter(publication: publication).toCommandObject()
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

        if (data["comesFromDB"] && operation == "add") {
            data["message"] = "The publication exists!"
            data["status"] = "Failed"
        } else if (!data["publication"]?.isEmpty() && operation == "edit") {
            boolean ID_EXISTS = params.containsKey("id")
            if (ID_EXISTS) {
                data["publication"]?.id = params.long("id")
            }
        }
        List linkSourceTypes = PLP.LinkType.values().collect { it.label }
        render(template: "/templates/publication/publicationDetailForm",
            plugin: "jummp-plugin-web-application",
            model: [id: params.id, publication: data["publication"], comesFromDB: data["comesFromDB"],
                    authorListContainerSize: 4, status: data["status"],
                    linkSourceTypes: linkSourceTypes, message: data["message"],
                    controller: "publication", operation: operation, url: request.forwardURI])
    }

    def save(PubTC pubCmd) {
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
        PubTC cmd = new PubTC()
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
                message = "The publication details have been fetched successfully."
                status = "OK"
                cmd = publicationService.createPTCWithMinimalInformation(pubLinkProvider, pubLink, [])
                PDEC ctx = loadOrFetchOrCreatePublication(cmd)
                // reassign cmd to a newly refreshed one
                cmd = ctx?.publication
                if (!cmd) {
                    status = "Unavailable"
                    message = "No record found. Please do check and try again."
                } else if (!cmd?.synopsis || !cmd?.affiliation || !cmd?.title) {
                    // for DOI fetched from DOI service or for PubMed entry not having any values for these fields
                    status = "Warning"
                    String t = cmd.linkProvider.linkType
                    String pubHref = "https://doi.org/${pubLink}"
                    if (t == PLP.LinkType.PUBMED.getLabel()) {
                        pubHref = "https://identifiers.org/pubmed/${pubLink}"
                    }
                    message = """The publication details are the best which our system can automatically
fetch from <a href="${pubHref}" target="_blank">${pubHref}</a>. Currently they are
missing a title, an affiliation and/or an abstract. Please verify the form and fill in the empty fields manually."""
                }

                comesFromDB = ctx?.comesFromDatabase
            }
        }
        ["message": message, "status": status, "publication": cmd, "comesFromDB": comesFromDB]
    }

    def doVerifyPublicationProviderAndLink() {
        PubTC cmd = new PubTC()
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
                message = "The publication provider and link are valid."
                status = "OK"
            }
        }
        render(["message": message, "status": status] as JSON)
    }

    def validatePublicationDetails() {
        Map result = publicationService.buildPublicationFromJSONData(params.pubDetails.decodeHTML())
        HashSet<String> changesMade = new HashSet<>()
        if (params.boolean("isUpdate")) {
            changesMade = inferChangesMadeOnModelPublicationDetails(result)
        } else {
            changesMade.addAll(["MODEL PUBLICATION: Added the publication details."])
        }
        result.put("changesMade", changesMade)
        render(result as JSON)
    }


    def renderPublicationDetails() {
        if (!params.pubDetails) {
            render("No publication provided")
        } else {
            // this action is often called to display the publication which has been validated
            // so we don't need to handle exception
            PubTC tempPTC = new PubTC()//pubContext.publication
            def pubDetails = JSON.parse(params.pubDetails.decodeHTML())
            bindData(tempPTC, pubDetails, [exclude: ['authors']])
            publicationService.assembleAuthors(tempPTC, pubDetails.authors)
            render(template: "/templates/showPublication", model: [publication: tempPTC, isUpdate: false])
        }
    }

    private HashSet<String> inferChangesMadeOnModelPublicationDetails(Map result) {
        HashSet<String> changesMade = params.list("changesMade[]").toSet()
        if (!changesMade) { changesMade = new HashSet<>() }
        if (params.boolean("isUpdate")) {
            String modelId = params.modelId.decodeHTML()
            PubTC pubTC = publicationService.findPublicationOfModel(modelId)
            if (!pubTC) {
                if (result["status"] == "Success" && result["publication"]) {
                    changesMade.addAll(["MODEL PUBLICATION: Added the publication details."])
                }
            } else {
                if (result["status"] == "Success" && result["publication"]) {
                    findUpdates(changesMade, pubTC, result["publication"])
                } else if (!result["publication"]) {
                    changesMade.addAll(["MODEL PUBLICATION: Removed the publication details."])
                }
            }
        }
        changesMade
    }

    private HashSet findUpdates(HashSet<String> changesMade, PubTC oldPub, PubTC newPub) {
        if (changesMade) {
            CollectionHelper.remove(changesMade, "MODEL PUBLICATION")
        }
        if (oldPub.link != newPub.link && oldPub.link &&newPub.link) {
            changesMade.add("MODEL PUBLICATION: Changed the publication link/identifier from ${oldPub.link} to ${newPub.link}.")
        }
        if (oldPub.title != newPub.title) {
            changesMade.add("MODEL PUBLICATION: Changed the publication title from ${oldPub.title} to ${newPub.title}.")
        }
        if (oldPub.journal != newPub.journal) {
            changesMade.add("MODEL PUBLICATION: Changed the publication journal from ${oldPub.journal} to ${newPub.journal}.")
        }
        if (oldPub.affiliation != newPub.affiliation) {
            changesMade.add("MODEL PUBLICATION: Changed the publication affiliation from ${oldPub.affiliation} to ${newPub.affiliation}.")
        }
        if (oldPub.year != newPub.year || oldPub.month != newPub.month || oldPub.day != newPub.day) {
            changesMade.add("MODEL PUBLICATION: Updated the publication date time.")
        }
        if (oldPub.volume != newPub.volume || oldPub.issue != newPub.issue) {
            changesMade.add("MODEL PUBLICATION: Updated the publication issue/volume.")
        }
        if (oldPub.pages != newPub.pages) {
            changesMade.add("MODEL PUBLICATION: Changed the publication pages from ${oldPub.pages} to ${newPub.pages}.")
        }
        if (oldPub.synopsis != newPub.synopsis) {
            changesMade.add("MODEL PUBLICATION: Edited the publication abstract.")
        }
        // TODO: diffs = findDifferences(oldPub.authors, newPub.authors), then changesMade.addAll(diffs)
        changesMade
    }

    private PDEC loadOrFetchOrCreatePublication(PubTC pubTC) {
        try {
            PDEC publicationContext = publicationService.getPublicationExtractionContext(pubTC)
            if (publicationContext.publication) {
                if (publicationContext.comesFromDatabase) {
                    flash.flashMessage = g.message(code: "publication.editor.duplicateEntry.message")
                }
            } else {
                PubTC retrieved
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
