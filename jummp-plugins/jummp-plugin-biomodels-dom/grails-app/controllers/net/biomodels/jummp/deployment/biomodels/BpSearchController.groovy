package net.biomodels.jummp.deployment.biomodels

import grails.plugin.springsecurity.annotation.Secured
import grails.rest.RestfulController

/**
 * Created by carankalle on 08/10/2018.
 */
@Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
class BpSearchController extends RestfulController {
    static responseFormats = ['json', 'xml']

    def index = {
        render(view: "/bpSearch/BpSearch.gsp");
    }

    def hello = {
        render(text:"Hello world")
    }
}
