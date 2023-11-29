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
import net.biomodels.jummp.plugins.security.User
import org.apache.commons.lang.math.NumberUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.InitializingBean

import java.text.SimpleDateFormat

/**
 * @short Service responsible for retrieving CurationNotes entries.
 * This class  is used for dealing with CurationNotes records.
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
@Transactional
class CurationNotesService implements InitializingBean {
    static final Log log = LogFactory.getLog(this.getClass())
    /**
     * Flag indicating the logger's verbosity threshold.
     */
    static final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()

    def userService
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
                latestCurationNotes = entries.first().toCommandObject()
            }
        }
        return latestCurationNotes
    }

    /**
     * Build up a map of essential parameters for the update and add operation, etc.
     * Depending on the operation in operating, the map could be or populated against an
     * existing curation notes or initialised a few attributes with the defaults values.
     *
     * @param a map   A map containing model identifier and curation notes identifier
     * @return a map  A map of the necessary parameters will be used in the other operations.
     */
    Map loadOrInitialise(Map args) {
        CurationNotesTransportCommand curationNotesTC = null
        if (args.containsKey("cnId") && NumberUtils.isNumber(args.get("cnId"))) {
            Long cnId = Long.parseLong(args.get("cnId"))
            use(CurationNotesCategory) {
                curationNotesTC = CurationNotes.get(cnId).toCommandObject()
            }
        }

        if (!curationNotesTC) {
            // this case is to add a new curation notes
            curationNotesTC = new CurationNotesTransportCommand()
            curationNotesTC.updated = false
            curationNotesTC.comment = null
            curationNotesTC.internalComment = null
            curationNotesTC.dateAdded = new Date()
            curationNotesTC.curationImage = null
            curationNotesTC.submitter = userService.getCurrentUser()
        } else {
            curationNotesTC.updated = true
        }
        curationNotesTC.lastModifier = userService.getCurrentUser()
        curationNotesTC.lastModified = new Date()
        String modelId = args.get("model")
        String curationImage
        curationImage = curationNotesTC.curationImage ? Base64.encoder.encodeToString(curationNotesTC.curationImage) : null
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        ['curationNotesTC': curationNotesTC, 'curationImage': curationImage,
         'dateFormat': dateFormat, 'id': modelId]
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

    CurationNotes doAddOrUpdateCurationNotes(CurationNotesTransportCommand cntc) {
        Model model = Model.get(cntc.model.id)
        User submitter = cntc.submitter
        User lastModifier = cntc.lastModifier
        Date dateAdded = cntc.dateAdded
        Date lastModified = cntc.lastModified
        String comment = cntc.comment
	    String internalComment = cntc.internalComment
        byte[] curationImage = cntc.curationImage
        Map criteria = [model: model, submitter: submitter, dateAdded: dateAdded]
        CurationNotes cn = CurationNotes.findOrSaveWhere(criteria)
        cn.lastModifier = lastModifier
        cn.lastModified = lastModified
        cn.comment = comment
        cn.internalComment = internalComment
        cn.curationImage = curationImage
        String modelId = model.publicationId ?: model.submissionId
        if (cn.save(flush: true)) {
            log.debug("The simulation results of the model $modelId have been saved!")
            return cn
        } else {
            log.error("""\
There are errors when trying to persist curation notes of the model $modelId into database: ${cn.errors.allErrors.inspect()}""")
            return null
        }
    }

    @Override
    void afterPropertiesSet() throws Exception {
        log.info("Finished the bean initialisation")
    }
}
