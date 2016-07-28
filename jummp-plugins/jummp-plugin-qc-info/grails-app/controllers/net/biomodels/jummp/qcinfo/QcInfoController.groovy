package net.biomodels.jummp.qcinfo

import grails.converters.JSON
import grails.plugins.springsecurity.Secured

@Secured(["isAuthenticated()"])
class QcInfoController {

    def modelDelegateService
    def qcInfoService

    def edit() {
        if (!params.id) {
            forward controller: "errors", action: "error404"
            return
        }

        def revision = modelDelegateService.getRevisionFromParams(params.id, params.revisionId)
        def modelId = (revision.model.publicationId) ?: (revision.model.submissionId)

        boolean canUpdate = modelDelegateService.canAddRevision modelId
        if (!canUpdate) {
            forward controller: 'errors', action: 'error403'
            return
        }

        def theModel = [
            revision: revision,
            modelId: modelId
        ]
        render model: theModel, view: 'edit'

    }

    def certify() {
        if(params.comment == "" ){
            render([status: '500', message: "Please add a comment."] as JSON)
            return
        }
        if(params.flag == "" ){
            render([status: '500', message: "Please select a certification level."] as JSON)
            return
        }

        boolean certified = false
        FlagLevel flag;
        if (params.flag == "1") {
            flag = FlagLevel.FLAG_1
        }
        else if (params.flag == "2") {
            flag = FlagLevel.FLAG_2
        }
        else if (params.flag == "3") {
            flag = FlagLevel.FLAG_3
        }else{
            render([status: '500', message: "Invalid certification level."] as JSON)
            return
        }

        def qcInfo = qcInfoService.createQcInfo(flag, params.comment)
        if (qcInfo != null) {
            certified = modelDelegateService.addQcInfo(params.revision, qcInfo)
        }
        if (certified) {
            render([status: '200', message: "The model is successfully certified."] as JSON)
        } else {
            render([status: '500', message: "Unable to certify the model."] as JSON)
        }
    }

}
