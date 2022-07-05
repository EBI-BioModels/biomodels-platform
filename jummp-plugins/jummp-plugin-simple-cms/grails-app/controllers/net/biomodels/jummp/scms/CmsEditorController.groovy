package net.biomodels.jummp.scms

import org.springframework.security.access.annotation.Secured
import net.biomodels.jummp.scms.CmsContentTransportCommand as CCTC
import net.biomodels.jummp.plugins.security.User

import java.text.SimpleDateFormat

@Secured(['ROLE_ADMIN', 'ROLE_CURATOR'])
class CmsEditorController {

    def cmsContentService

    def index() {
        render "index"
    }

    def edit() {
        Long id = params.long("id")
        if (!id) {
            render(controller: "errors", view: "error404")
            return
        } else {
            CCTC cntCmd = new CCTC(id: id)
            CmsContent content = cmsContentService.findByTransportCommand(cntCmd)
            if (!content) {
                String resource = "/cms/edit/$id"
                render(plugin: "jummp-plugin-web-application", controller: "errors", view: "error404",
                    model: [resource: resource])
                return false
            }
            cntCmd = CCTC.toCommandObject(content)
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            [id: content.id, content: cntCmd, dateFormat: dateFormat]
        }
    }
}
