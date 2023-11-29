/**
 * Copyright (C) 2010-2019 EMBL-European Bioinformatics Institute (EMBL-EBI),
 * Deutsches Krebsforschungszentrum (DKFZ)
 *
 * This file is part of Jummp.
 *
 * Jummp is free software you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation either version 3 of the License, or (at your option) any
 * later version.
 *
 * Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along
 * with Jummp if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
 */

package net.biomodels.jummp.deployment.biomodels

import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * Service for accessing the Reactome pathway mapping information.
 *
 * This is a prototype-scoped service which holds the model-pathway mapping information.
 *
 * @author carankalle on 26/11/2019.
 * @author mglont on 05/12/2019.
 * @see {@link net.biomodels.jummp.models.ReactomeServiceFactoryBean}
 */
class ReactomeService implements ApplicationContextAware, InitializingBean {
    ApplicationContext applicationContext
    Map<String, List<String>> modelPathwayMapping

    ReactomeService(Map<String, List<String>> modelPathwayMapping) {
        this.modelPathwayMapping = modelPathwayMapping
    }

    List<String> getPathwaysForModelId(String modelId) {
        modelPathwayMapping.get(modelId)
    }

    @Override
    void afterPropertiesSet() throws Exception {
        log.info("Finished the bean initialisation")
    }
}
