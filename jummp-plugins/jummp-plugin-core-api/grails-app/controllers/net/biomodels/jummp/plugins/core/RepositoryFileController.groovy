/**
 * Copyright (C) 2010-2019 EMBL-European Bioinformatics Institute (EMBL-EBI),
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





package net.biomodels.jummp.plugins.core

import grails.plugin.springsecurity.annotation.Secured

/**
 * @author  Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
@Secured(['IS_AUTHENTICATED_FULLY'])
class RepositoryFileController {
    def repositoryFileService

    def index() { }

    def updateDescription() {
        String repoFile = params?.repoFileId
        Long repoFileId = Long.parseLong(repoFile)
        String fileName = params?.path
        String revisionId = params?.revision
        Long revision = Long.parseLong(revisionId)
        String description = params?.description
        Boolean response = repositoryFileService.updateDescription(repoFileId, revision, fileName, description)
        String message = "The description has been updated successfully"
        if (response == Boolean.FALSE) {
            message = "There is an error while trying to update the description"
        }
        render message
    }
}
