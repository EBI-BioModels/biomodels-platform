package net.biomodels.jummp.webapp.administration

import grails.plugin.springsecurity.annotation.Secured
import net.biomodels.jummp.deployment.biomodels.CommonController

@Secured('ROLE_CURATOR')
class CurationController extends CommonController {

    def dashboard() {
        String title = "Curation Dashboard | BioModels"
        [layout: layout, title: title]
    }
}
