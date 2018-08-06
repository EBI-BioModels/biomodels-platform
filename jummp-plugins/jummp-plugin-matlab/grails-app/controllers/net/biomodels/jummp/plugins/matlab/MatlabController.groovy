package net.biomodels.jummp.plugins.matlab

import net.biomodels.jummp.core.annotation.QualifierTransportCommand
import net.biomodels.jummp.core.annotation.ResourceReferenceTransportCommand

class MatlabController {

    def matlabService
    def metadataDelegateService

    def show() {
        def model = flash.genericModel
        final def revision = model.revision
        Set<File> matlabFiles = matlabService.getMatlabFilesFromRevision revision
        Map<QualifierTransportCommand, List<ResourceReferenceTransportCommand>> anno =
                metadataDelegateService.fetchGenericAnnotations(model.revision)

        model['annotations'] = anno
        model['matlabFiles'] = matlabFiles
        render model: model, view: '/model/matlab/show', plugin: 'jummp-plugin-matlab'
    }
}
