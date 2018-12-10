/**
 * Copyright (C) 2010-2016 EMBL-European Bioinformatics Institute (EMBL-EBI),
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





package net.biomodels.jummp.plugins.omicsdi

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured

@Secured(['ROLE_ADMIN', 'ROLE_CURATOR'])
class OmicsdiController {
    /**
     * Flag that checks whether the dynamically-inserted logger is set to INFO or higher.
     */
    private final boolean IS_INFO_ENABLED = log.isInfoEnabled()

    def omicsdiService

    def index() {
        render(view: "index")
    }

    def exportOmicsdiEntriesWithIndexer() {
        if (IS_INFO_ENABLED) {
            log.info "Delegating this work to JummpIndexer."
        }
        boolean allowMultipleFilesParam = (params.howToExportFile == "1") ? false : true
        int numberEntriesOnEachFile = 0
        if (params.numberEntriesOnEachFile != "") {
            numberEntriesOnEachFile = Integer.parseInt(nbEntries)
        }
        def options = [
            'allowMultipleFiles': allowMultipleFilesParam,
            'numberEntriesOnEachFile': numberEntriesOnEachFile
        ]
        omicsdiService.exportOmicsdiEntries(options)
        render(["Sent the request to JummpIndexer with the options ${options}"] as JSON)
    }
}
