package net.biomodels.jummp.webapp

import net.biomodels.jummp.core.annotation.QualifierTransportCommand
import net.biomodels.jummp.core.annotation.ResourceReferenceTransportCommand

class AnnotationTagLib {
    static namespace = "anno"

    def annotationRenderingTemplateProvider

    static defaultEncodeAs = [taglib: 'none']
    //static encodeAsForTags = [tagName: [taglib:'html'], otherTagName: [taglib:'none']]

    /**
     * Renders model-level annotations on the 'Overview' tab of the model display.
     *
     * @attr annotations REQUIRED mapping from qualifier to list of resource references
     * that should be rendered.
     */
    def renderGenericAnnotations = { attrs ->
        Map<QualifierTransportCommand, List<ResourceReferenceTransportCommand>> anno = attrs.annotations
        String templateName = annotationRenderingTemplateProvider.template
        String tpl = "/annotation/$templateName"
        out << g.render(template: tpl, plugin: "jummp-plugin-web-application", model: [annotations: anno])
    }
}
