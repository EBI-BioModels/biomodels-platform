package net.biomodels.jummp.scms

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.access.annotation.Secured

@Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
class ContentController {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass())

    def grailsApplication
    def cmsContentService

    def index() {
        render("Test")
    }

    def showModelOfTheMonth() {
        String taglibFQN = 'net.biomodels.jummp.deployment.biomodels.BioModelsTagLib'
        def biomd = grailsApplication.mainContext.getBean(taglibFQN)
        Map data = [:]
        if (params.containsKey("all") && params.get("all") == "yes") {
            data["content"] = biomd.renderAllMoMEntriesPage()
            data.put("pageTitle", "All entries of Model of the Month")
        } else if (params.containsKey("year") && (params.containsKey("month"))){
            String strYear = params.get("year")
            String strMonth = params.get("month")
            String aliasURI = "$strYear-$strMonth".toString()
            LOGGER.info("Accessing the model of month: $aliasURI")
            data = cmsContentService.loadContentByAliasURI(aliasURI, "MoM")
            data.put("pageTitle", "Model of $strMonth - $strYear")
        } else {
            forward(controller: "errors", action: "error404", plugin: "jummp-plugin-web-application")
            return
        }

        if (!data["content"]) {
            forward(controller: "errors", action: "error404", plugin: "jummp-plugin-web-application")
            return
        }
        render(view: "show", model: data)
    }

    def showNewsItem() {
        String slug = params.get("slug")
        LOGGER.info("Accessing the news item: $slug")
        Map data = cmsContentService.loadContentByAliasURI(slug, "news")
        if (!data["content"]) {
            LOGGER.debug("Cannot find the news item: $slug")
            forward(controller: "errors", action: "error404", plugin: "jummp-plugin-web-application")
            return
        }
        data.put("pageTitle", data.get("title"))
        render(view: "show", model: data)
    }
}
