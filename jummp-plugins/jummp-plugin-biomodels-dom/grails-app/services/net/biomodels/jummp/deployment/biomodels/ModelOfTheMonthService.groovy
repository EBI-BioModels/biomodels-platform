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
import org.apache.commons.io.IOUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import java.text.SimpleDateFormat

/**
 * @short Service responsible for retrieving BioModels ModelOfTheMonth entries.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
@Transactional(readOnly = true)
class ModelOfTheMonthService {
    /**
     * The class logger.
     */
    private static final Log log = LogFactory.getLog(ModelOfTheMonth.class)
    /**
     * Threshold for the verbosity of the logger.
     */
    private static final boolean IS_INFO_ENABLED = log.isInfoEnabled()
    /**
     * Threshold for the verbosity of the logger.
     */
    private static final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()

    List fetchEntriesForModel(Long id) {
        List entries = ModelOfTheMonth.withCriteria {
            models {
                eq "id", id
            }
        }
        entries*.toCommandObject()
    }

    List<ModelOfTheMonth> list() {
        ModelOfTheMonth.getAll()
    }
    /**
     * Update the preview image and short description of a given model of the month entry
     */
    @Transactional(readOnly=false)
    ModelOfTheMonth updatePreviewImageAndShortDescription(Long id, byte[] previewImage, String shortDescription) {
        ModelOfTheMonth model = ModelOfTheMonth.findById(id)
        if (model) {
            model.previewImage = previewImage
            model.shortDescription = shortDescription
            ModelOfTheMonth updatedModel = model.save(flush: true)
            log.info "${model.id}: ${model.shortDescription}"
            if (!model.save(flush: true)) {
                log.debug("Errors at trying to save MoM: ${model.errors.allErrors.toString()}")
                return null
            } else {
                return model
            }
        } else {
            log.debug("Errors at trying to save MoM: ${model.errors.allErrors.toString()}")
            return null
        }
    }

    /**
     * Try to update the preview image and short description for entire model of the month entries if
     * they haven't been attached these information
     */
    @Transactional(readOnly=false)
    List<ModelOfTheMonth> updatePreviewImageAndShortDescription() {
        String prefixUrl = "http://www.ebi.ac.uk/biomodels/ModelMonth/"
        List<ModelOfTheMonth> modelOfTheMonths = ModelOfTheMonth.getAll()
        List<ModelOfTheMonth> results = []
        SimpleDateFormat dateFormat = new SimpleDateFormat('yyyy-MM')
        modelOfTheMonths.each { ModelOfTheMonth model ->
            Date publicationDate = model.publicationDate
            String momFolder = dateFormat.format(publicationDate) // this is the folder pattern of where is storing MoM entry
            String momFolderLink = "${prefixUrl}/${momFolder}"
            String imageFileName = "preview.png"
            String momImageLink = "${momFolderLink}/${imageFileName}"
            URL imageURL = new URL(momImageLink)
            int responseCode = imageURL.openConnection().getResponseCode()
            byte[] previewImage = []
            String shortDescription = ""
            if (responseCode == 200) {
                def InputStream is = new BufferedInputStream(imageURL.openStream())
                byte[] bytes = IOUtils.toByteArray(is)
                previewImage = bytes
            }
            String momShortDescriptionLink = "${momFolderLink}/briefdescrib"
            URL textURL = new URL(momShortDescriptionLink)
            responseCode = textURL.openConnection().getResponseCode()
            if (responseCode == 200) {
                def InputStream is = new BufferedInputStream(textURL.openStream())
                List<String> text = is.readLines()
                shortDescription = text.first()
            }
            results << updatePreviewImageAndShortDescription(model.id, previewImage, shortDescription)
        }
        results
    }
}

