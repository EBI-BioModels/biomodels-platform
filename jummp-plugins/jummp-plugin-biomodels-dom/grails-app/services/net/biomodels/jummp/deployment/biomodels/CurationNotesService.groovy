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
        if (entries.size() > 0) {
            use(CurationNotesCategory) {
                latestCurationNotes = entries.first()?.toCommandObject()
            }
        }
        return latestCurationNotes
    }

    /**
     * Update curation image as a part of the simulation results
     * @param modelSubmissionId A string represents model identifier
     * @param newCuraImg        A string represents the image encoded as Base64
     */
    boolean updateCurationImage(String modelSubmissionId, String newCuraImg) {
        Model model = Model.findBySubmissionIdOrPublicationId(modelSubmissionId, modelSubmissionId)
        Long modelId = model.id
        byte[] newCuraImgByteArr = Base64.decoder.decode(newCuraImg)
        updateCurationImage(modelId, newCuraImgByteArr)
    }

    boolean updateCurationImage(Long modelId, byte[] newCuraImg) {
        CurationNotesTransportCommand cntc = fetchCurationNotesForModel(modelId)
        CurationNotes cn
        Model model = Model.get(modelId)
        if (cntc) {
            cn = CurationNotes.findBySubmitterAndLastModifierAndModel(cntc.submitter, cntc.lastModifier, model)
            cn.curationImage = newCuraImg
        } else {
            cn = new CurationNotes()
            cn.curationImage = newCuraImg
        }
        boolean success = cn.save(flush: true)
        if (success) {
            log.debug("""\
The curation image associated with the simulation results of model $model.submissionId
has been saved successfully into the database.""")
        } else {
            log.error("""\
There is an error when trying to persist curate image into database: ${cn.errors.allErrors.inspect()}""")
        }
        success
    }

    boolean doAddOrUpdateCurationNotes(CurationNotesTransportCommand cntc) {
        Model model = Model.get(cntc.model.id)
        Long id = cntc.id
        CurationNotes cn = CurationNotes.get(id)
        if (!cn) {
            cn = new CurationNotes()
        }
        cn.model = model
        cn.comment = cntc.comment
        cn.submitter = cntc.submitter
        cn.lastModifier = cntc.lastModifier
        cn.dateAdded = cntc.dateAdded
        cn.lastModified = cntc.lastModified
        cn.curationImage = cntc.curationImage
        if (cn.save(flush: true)) {
            log.debug("The simulation results of the model $model.id have been saved!")
            return true
        } else {
            log.error("""\
Failed to try to persist curation notes of the model $model.id into database: ${cn.errors.allErrors.inspect()}""")
            return false
        }
    }
}
