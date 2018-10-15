package net.biomodels.jummp.deployment.biomodels

import grails.plugin.springsecurity.annotation.Secured

/**
 * Created by carankalle on 08/10/2018.
 */
@Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
class ParameterSearchController {
    def index = {
        render(view: "index.gsp");
    }
}
