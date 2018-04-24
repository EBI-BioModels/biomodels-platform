package net.biomodels.jummp.plugins.configuration

import grails.plugin.springsecurity.annotation.Secured

@Secured(["hasRole('ROLE_ADMIN')"])
class ClassifierConfigureController {

    def index() { }

    def classifier = {
        render(view: 'configuration', model: [title: "Model Classification", action: "saveConfigure", template: "classifier"])
    }
}
