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
 */





package net.biomodels.jummp.deployment.biomodels

import grails.transaction.Transactional

/**
 * @short Service responsible for retrieving CurationNotes entries.
 *
 * This class  is used for dealing with CurationNotes records.
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
@Transactional
class CurationNotesService {
    /**
     * Retrieve the latest modified curation notes based on given model id
     *
     * @param modelId The given model identifier
     * @return A CurationNotesTransportCommand that represents of the latest modified CurationNotes
     */
    CurationNotesTransportCommand fetchCurationNotesForModel(Long modelId) {
        List entries = CurationNotes.withCriteria {
            model {
                eq "id", modelId
            }
        }
        CurationNotesTransportCommand latestCurationNotes = null
        if (!entries.isEmpty()) {
            // assume the first element as the deliberately returned one
            CurationNotes proposedCurationNotes = entries[0]
            Date lastModifiedDate = proposedCurationNotes.lastModified
            use(CurationNotesCategory) {
                latestCurationNotes = proposedCurationNotes.toCommandObject()
            }
            // determine the latest curation notes
            use(CurationNotesCategory) {
                entries.each { CurationNotes e ->
                    if (e.lastModified > lastModifiedDate) {
                        lastModifiedDate = e.lastModified
                        latestCurationNotes = e.toCommandObject()
                    }
                }
            }
        }
        return latestCurationNotes
    }
}
