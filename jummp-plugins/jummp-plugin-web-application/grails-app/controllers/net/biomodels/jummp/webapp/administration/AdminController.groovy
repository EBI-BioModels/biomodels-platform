package net.biomodels.jummp.webapp.administration

import grails.plugin.springsecurity.annotation.Secured
import net.biomodels.jummp.deployment.biomodels.CommonController

@Secured('ROLE_ADMIN')
class AdminController extends CommonController {

    def dashboard() {
        String title = "Administration Dashboard | BioModels"
        [layout: layout, title: title]
    }
}
