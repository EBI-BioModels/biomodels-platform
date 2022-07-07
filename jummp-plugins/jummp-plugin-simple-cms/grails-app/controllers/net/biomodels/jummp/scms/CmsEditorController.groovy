/**
 * Copyright (C) 2010-2022 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
 */

package net.biomodels.jummp.scms

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.access.annotation.Secured
import net.biomodels.jummp.scms.CmsContentTransportCommand as CCTC
import net.biomodels.jummp.plugins.security.User

import java.text.SimpleDateFormat

@Secured(['ROLE_ADMIN', 'ROLE_CURATOR'])
class CmsEditorController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CmsEditorController.class)

    def cmsContentService
    def userService

    def index() {
        render "index"
    }

    def edit() {
        LOGGER.debug("Started editing content...")
        println("Started editing content...")
        Long id = params.long("id")
        Map data = cmsContentService.findDataAndRenderView(id, "edit")
        render(plugin: data.plugin, controller: data.controller, view: data.view, model: data.model)
    }
}
