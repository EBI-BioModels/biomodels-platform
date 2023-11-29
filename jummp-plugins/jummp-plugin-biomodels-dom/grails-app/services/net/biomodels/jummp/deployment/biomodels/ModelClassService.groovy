/**
 * Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
 *
 * Additional permission under GNU Affero GPL version 3 section 7
 *
 * If you modify Jummp, or any covered work, by linking or combining it with
 * Spring Security (or a modified version of that library), containing parts
 * covered by the terms of Apache License v2.0, the licensors of this
 * Program grant you additional permission to convey the resulting work.
 * {Corresponding Source for a non-source form of such a combination shall
 * include the source code for the parts of Spring Security used as well as
 * that of the covered work.}
 **/



package net.biomodels.jummp.deployment.biomodels

import net.biomodels.jummp.model.Model
import net.biomodels.jummp.models.ModelClassStatus
import net.biomodels.jummp.plugins.security.User
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean

/**
 * @short: Service responsible for management model class ground truth data
 * @author: Vu Tu <tvu@ebi.ac.uk>
 */
class ModelClassService implements InitializingBean {

    static transactional = false

    private static final Logger LOGGER = LoggerFactory.getLogger(ModelClassService.class)

    List<ModelClass> getModelClasses() {
        return ModelClass.findAll()
    }

    /**
     * Save ground truth mode record made by curator
     *
     * @param realCategory
     * @param model
     * @param user
     */
    void saveGroundTruth(String realCategory, Model model, User user) {

        ModelClass modelClass = ModelClass.findByModel(model)
        if (modelClass == null) {
            modelClass = new ModelClass()
            modelClass.setModel(model)
            modelClass.setCreatedBy(user)
            modelClass.setCreatedDate(new Date())
        }
        modelClass.setClassName(realCategory)
        modelClass.setStatus(ModelClassStatus.UNTRAINED)
        modelClass.setUpdatedBy(user)
        modelClass.setUpdatedDate(new Date())
        if (!modelClass.save(flush: true)) {
            LOGGER.error("An error occurred when trying to persist the model class {}",
                modelClass.errors.allErrors.inspect())
        }
    }

    void deleteGroundTruth(Model model) {
        ModelClass modelClass = ModelClass.findByModel(model)
        if (modelClass) {
            modelClass.delete(flush: true)
        }
    }

    @Override
    void afterPropertiesSet() throws Exception {
        LOGGER.info("Finished the bean initialisation")
    }
}
