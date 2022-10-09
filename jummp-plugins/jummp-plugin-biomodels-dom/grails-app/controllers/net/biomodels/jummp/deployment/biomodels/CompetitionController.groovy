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

package net.biomodels.jummp.deployment.biomodels

import grails.plugin.springsecurity.annotation.Secured

@Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
class CompetitionController {
    def featureService
    def cmsContentService

    def modelOfTheYear2022() {
        List result = featureService.getContentForModelOfTheYear2022CompetitionPage()
        Long id = result[0] ?: null
        String content = result[1] ?: ""
        boolean canEdit = cmsContentService.canEdit()
        String titlePage = "Model Of The Year 2022 Competition | BioModels"
        render(view: "model-of-the-year",
            model: [id: id, content: content, canEdit: canEdit, titlePage: titlePage])
    }

    def modelOfTheYear2023() {
        List result = featureService.getContentForModelOfTheYear2023CompetitionPage()
        Long id = result[0] ?: null
        String content = result[1] ?: ""
        boolean canEdit = cmsContentService.canEdit()
        String titlePage = "Model Of The Year 2023 Competition | BioModels"
        render(view: "model-of-the-year",
            model: [id: id, content: content, canEdit: canEdit, titlePage: titlePage])
    }
}
