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
import net.biomodels.jummp.model.Model
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * @short Service responsible for retrieving CurationNotes entries.
 * This class  is used for dealing with CurationNotes records.
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
@Transactional
class CurationNotesService {
    static final Log log = LogFactory.getLog(this.getClass())
    /**
     * Flag indicating the logger's verbosity threshold.
     */
    static final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()
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
            order("lastModified", "desc")
        }
        CurationNotesTransportCommand latestCurationNotes = null
        use(CurationNotesCategory) {
            latestCurationNotes = entries.first().toCommandObject()
        }
        return latestCurationNotes
    }

    /**
     * Update curation image as a part of the simulation results
     * @param modelSubmissionId A string represents model identifier
     * @param newCuraImg        A string represents the image encoded as Base64
     */
    void updateCurationImage(String modelSubmissionId, String newCuraImg) {
        Model model = Model.findBySubmissionIdOrPublicationId(modelSubmissionId, modelSubmissionId)
        Long modelId = model.id
        byte[] newCuraImgByteArr = Base64.decoder.decode(newCuraImg)
        updateCurationImage(modelId, newCuraImgByteArr)
    }

    void updateCurationImage(Long modelId, byte[] newCuraImg) {
        CurationNotesTransportCommand cntc = fetchCurationNotesForModel(modelId)
        if (cntc) {
            Model model = Model.get(modelId)
            CurationNotes cn = CurationNotes.findBySubmitterAndLastModifierAndModel(cntc.submitter, cntc.lastModifier, model)
            cn.curationImage = newCuraImg
            cn.save(flush: true)
        }
    }

    boolean updateCurationNotes(CurationNotesTransportCommand cntc) {
        Model model = Model.get(cntc.model.id)
        Long id = cntc.id
        CurationNotes cn = CurationNotes.get(id)
        if (cn) {
            cn.comment = cntc.comment
            cn.submitter = cntc.submitter
            cn.lastModifier = cntc.lastModifier
            cn.dateAdded = cntc.dateAdded
            cn.lastModified = cntc.lastModified
            if (cn.save(flush: true)) {
                if (IS_DEBUG_ENABLED) {
                    log.debug("The simulation results of the model $model.id have been saved!")
                }
                return true
            } else {
                throw new Exception("Failed to try persisting curation notes")
            }
        }
        false
    }
}
