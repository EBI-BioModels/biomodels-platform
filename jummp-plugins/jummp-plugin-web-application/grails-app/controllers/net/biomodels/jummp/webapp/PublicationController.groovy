package net.biomodels.jummp.webapp

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import net.biomodels.jummp.core.InvalidPublicationAuthorsException
import net.biomodels.jummp.core.adapters.PublicationAdapter
import net.biomodels.jummp.core.adapters.PublicationLinkProviderAdapter
import net.biomodels.jummp.model.Publication

import net.biomodels.jummp.core.model.PublicationTransportCommand
import net.biomodels.jummp.model.PublicationLinkProvider as PLP
import org.codehaus.groovy.grails.plugins.support.aware.GrailsConfigurationAware
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Secured(['IS_AUTHENTICATED_FULLY'])
class PublicationController implements GrailsConfigurationAware {
    private static final Logger logger = LoggerFactory.getLogger(PublicationController.class)
    def publicationService
    def pubMedService
    def messageSource
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
            model: [id: params.id, publication: pubTC,
                    authorListContainerSize: 4, controllerName: "publication",
                    actionName: "show"]
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
        println "pub link provider: $pubLinkProvider"
        println "pub link: $pubLink"
        if (pubLinkProvider == "NoPub" && pubLink) {
            message = "Please select a publication link type."
            status: "Failed"
        }
        if (!publicationService.verifyLink(pubLinkProvider, pubLink)) {
            message = "The link is not a valid ${pubLinkProvider}"
            status = "Failed"
        } else {
            message = "The publication link provider and link are valid"
            status = "OK"
            if (pubLinkProvider == "PubMed ID") {
                cmd = pubMedService.fetchPublicationData(pubLink)
            } else {
                def provider = PLP.LinkType.findLinkTypeByLabel(pubLinkProvider)
                PLP publicationLinkProvider = PLP.withCriteria(uniqueResult: true) {
                    eq("linkType", provider)
                } as PLP
                cmd.link = pubLink
                cmd.linkProvider = new PublicationLinkProviderAdapter(linkProvider:
                    publicationLinkProvider).toCommandObject()
            }
        }
        render(["message": message, "status": status, "publication": cmd] as JSON)
    }

    def validatePublicationDetails() {
        //PDEC pubContext = publicationMap.get(flow.workingMemory.get("SelectedPubLinkProvider"))
        PublicationTransportCommand tempPTC = new PublicationTransportCommand()//pubContext.publication
        def pubDetails = JSON.parse(params.pubDetails.decodeHTML())
        bindData(tempPTC, pubDetails, [exclude: ['authors', 'linkProvider']])
        tempPTC.linkProvider = publicationService.inferPublicationLinkProvider(pubDetails.linkProvider)
        String message = ""
        String status = ""
        List errors = new ArrayList()
        try  {
            publicationService.assembleAuthors(tempPTC, pubDetails.authors)
            message = "Authors have been successfully assembled"
            status = "Success"
        } catch (InvalidPublicationAuthorsException e) {
            String errMsg = e.getI18nErrorMessage4InvalidAuthor()
            message = "There have been errors while parsing authors of the publication:<br/>${errMsg}"
            status = "Error"
        }
        if (tempPTC.hasErrors()) {
            def locale = Locale.getDefault()
            for (fieldErrors in tempPTC.errors) {
                for (error in fieldErrors.allErrors) {
                    message = messageSource.getMessage(error, locale)
                    errors.add(message)
                    logger.error(message)
                }
            }
            status = "Error"
        }
        render(["message": message, "status": status, "errors": errors] as JSON)
    }

    def renderPublicationDetails() {
        // this action is often called to display the publication which has ben validated
        // so we don't need to handle exception
        PublicationTransportCommand tempPTC = new PublicationTransportCommand()//pubContext.publication
        def pubDetails = JSON.parse(params.pubDetails.decodeHTML())
        bindData(tempPTC, pubDetails, [exclude: ['authors']])
        //tempPTC.linkProvider = publicationService.inferPublicationLinkProvider(pubDetails.linkProvider)
        publicationService.assembleAuthors(tempPTC, pubDetails.authors)
        render(template: "/templates/showPublication", model: [publication: tempPTC, isUpdate: false])
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
