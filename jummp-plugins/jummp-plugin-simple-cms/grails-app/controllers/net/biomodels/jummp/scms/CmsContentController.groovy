package net.biomodels.jummp.scms

import grails.converters.JSON
import net.biomodels.jummp.scms.CmsContentTransportCommand as CCTC
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.access.annotation.Secured
import net.biomodels.jummp.plugins.security.User

import java.text.SimpleDateFormat


@Secured(['ROLE_ADMIN', 'ROLE_CURATOR'])
class CmsContentController {
    private static final Logger LOGGER = LoggerFactory.getLogger(this.getClass())

    def cmsContentService

    def create() {
        // -1 is a fake id that will be granted a valid value
        CCTC content = new CCTC(id: -1,
            createdBy: "tung", createdOn: new Date(),
            lastChangedBy: "tung", lastChangedOn: new Date())
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

        [content: content, dateFormat: dateFormat]
    }

    def save(CCTC cmd) {
        Map result = [:]
        LOGGER.debug(cmd.dump())

        String message = ""
        String status = ""
        if (cmd?.validate()) {
            Map contentMap = cmsContentService.fromCommandObject(cmd)
            CmsContent content = contentMap.get("content")
            message = contentMap.get("message")
            if (message == "Success") {
                status = "Succeeded"
                message = "Saved content successfully"
            } else {
                status = "Failed"
                message = "Failed to save content"
            }
        } else {
            status = "Succeeded"
            message = "Data invalid: ${cmd.errors.toString()}"
        }
        result.put("status", status)
        result.put("message", message)
        render(result as JSON)
    }
}
