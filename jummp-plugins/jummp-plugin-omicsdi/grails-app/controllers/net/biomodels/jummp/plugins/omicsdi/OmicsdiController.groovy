package net.biomodels.jummp.plugins.omicsdi

import grails.converters.XML
import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured

@Secured(["hasRole('ROLE_ADMIN')"])
class OmicsdiController {

    def omicsdiService

    def index() {
        omicsdiService.saveAsOmicsdiSchemaXML()
        String content = omicsdiService.buildOmicsdiSchemaXml()
        render(view: "index", model: [schemaXmlPath: omicsdiService.loadSchemaXml(), nbmodels: content])
    }
}
