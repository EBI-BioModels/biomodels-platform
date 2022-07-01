package net.biomodels.jummp.scms

import org.springframework.security.access.annotation.Secured


@Secured(['ROLE_ADMIN', 'ROLE_CURATOR'])
class CmsEditorController {

    def index() {
        render "index"
    }

    def edit() {

    }
}
