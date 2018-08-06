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





package net.biomodels.jummp.core

import grails.transaction.Transactional
import net.biomodels.jummp.model.Flag
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.ModelFlag
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * @short   Simple representation of a model flag
 *
 * This class captures model flag
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
@Transactional
class ModelFlagService {
    /**
     * The class logger.
     */
    static final Log log = LogFactory.getLog(ModelFlagService.class)
    /**
     * Flag indicating the logger's verbosity threshold.
     */
    static final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()
    /**
     * Flag indicating the logger's verbosity threshold.
     */
    static final boolean IS_INFO_ENABLED = log.isInfoEnabled()
    /**
     * Flag indicating the logger's verbosity threshold.
     */
    static final boolean IS_ERROR_ENABLED = log.isErrorEnabled()

    List<Flag> getFlags(Model model) {
        List<Flag> result = new ArrayList<Flag>()
        List<ModelFlag> modelFlags = ModelFlag.findAllByModel(model)
        modelFlags.each {
            result.add(it.flag)
        }
        return result
    }

    List<Model> getModels(Flag flag) {
        List<Model> result = new ArrayList<Model>()
        List<ModelFlag> modelFlags = ModelFlag.findAllByFlag(flag)
        modelFlags.each {
            result.add(it.model)
        }
        return result
    }

    ModelFlag flagModel(Model model, Flag flag) {
        return saveOrUpdateModelFlag(model, flag)
    }

    Flag saveFlag(String label, String description, byte[] icon) {
        return saveOrUpdateFlag(label, description, icon)
    }

    private ModelFlag saveOrUpdateModelFlag(Model model, Flag flag) {
        def existingModelFlag = ModelFlag.findOrSaveByModelAndFlag(model, flag)
        return existingModelFlag
    }

    private Flag saveOrUpdateFlag(String label, String description, byte[] icon) {
        Flag flag = new Flag(label: label, description: description, icon: icon)
        return saveOrUpdateFlag(flag)
    }

    private Flag saveOrUpdateFlag(Flag flag) {
        Flag existingFlag = Flag.findById(flag.id)
        if (!existingFlag) {
            Flag newFlag = new Flag(label: flag.label, description: flag.description, icon: flag.icon)
            if (!newFlag.save(flush: true)) {
                if (IS_ERROR_ENABLED) {
                    log.error("Failed to save the flag $newFlag")
                }
                return null
            }
            if (IS_INFO_ENABLED) {
                log.info("Flag ${flag} has been saved successfully in the database.")
            }
            return newFlag
        }
        if (IS_DEBUG_ENABLED) {
            log.debug("Flag ${flag} exists.")
        }
        return existingFlag
    }
}
