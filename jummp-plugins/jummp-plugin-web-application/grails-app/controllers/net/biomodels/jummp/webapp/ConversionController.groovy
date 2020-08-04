/**
 * Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
 * Deutsches Krebsforschungszentrum (DKFZ)
 *
 * This file is part of Jummp.
 *
 * Jummp is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along
 * with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
 **/





/**
 * Controller for handling Conversion service (i.e. export/convert the model to the other formats).
 *
 * @author  Tung Nguyen <tung.nguyen@ebi.ac.uk>
 * @date    20180321
 */
package net.biomodels.jummp.webapp

import grails.plugin.springsecurity.annotation.Secured
import grails.util.Holders
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
import org.springframework.security.access.AccessDeniedException

@Secured(['IS_AUTHENTICATED_FULLY'])
class ConversionController {
    def modelDelegateService
    def modelConversionService

    @Secured(['ROLE_CURATOR', 'ROLE_ADMIN'])
    def convert() {
        RevisionTransportCommand revisionTC
        try {
            String modelId = params.id
            String revisionId = params.revisionId
            revisionTC = modelDelegateService.getRevisionFromParams(modelId, revisionId)
            boolean isServiceOn = modelConversionService.isAlive()
            if (isServiceOn) {
                modelConversionService.generateExports(revisionTC)
                redirect(controller: "model", action: "showWithMessage",
                    id: revisionTC.identifier(),
                    params: [flashMessage: """The request of converting the model ${revisionTC.identifier()}
to the other formats has been sent to the external conversion service."""])
            } else {
                redirect(controller: "model", action: "showWithMessage",
                    id: revisionTC.identifier(),
                    params: [flashMessage: """The model conversion service is temporarily unavailable for processing the request."""])
            }
        } catch(AccessDeniedException e) {
            log.error(e.message, e)
            forward(plugin: "jummp-plugin-web-application", controller: "errors", action: "error403")
        } catch(IllegalArgumentException e) {
            log.error(e.message)
            redirect(controller: "model", action: "showWithMessage",
                id: revisionTC.identifier(),
                params: [
                    flashMessage: """\
                        Model couldn't export to the other format. Try again, please!"""
                ])
        }
    }

    @Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
    def download() {
        final String EXPORT_FOLDER = modelConversionService.EXPORT_FOLDER
        String modelId = params.id
        String revisionId = params.revisionId
        String fileName = params.fileName
        String mimeType = params.mimeType
        String revisionFolder = "${EXPORT_FOLDER}${File.separator}"
        revisionFolder += "${modelId}${File.separator}${revisionId}${File.separator}"
        File file = new File(revisionFolder, fileName)
        response.setContentType(mimeType)
        final String INLINE = "attachment"
        response.setHeader("Content-disposition", "${INLINE};filename=\"${fileName}\"")
        byte[] fileData = file.readBytes()
        int previewSize = grailsApplication.config.jummp.web.file.preview as Integer
        response.outputStream << new ByteArrayInputStream(fileData)

    }
}
