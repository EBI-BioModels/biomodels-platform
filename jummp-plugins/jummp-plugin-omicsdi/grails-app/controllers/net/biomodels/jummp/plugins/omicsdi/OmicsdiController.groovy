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

import grails.plugin.springsecurity.annotation.Secured

@Secured(["hasRole('ROLE_ADMIN')"])
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
        omicsdiService.exportOmicsdiEntries()
        render "Sent the request to JummpIndexer"
    }
}
