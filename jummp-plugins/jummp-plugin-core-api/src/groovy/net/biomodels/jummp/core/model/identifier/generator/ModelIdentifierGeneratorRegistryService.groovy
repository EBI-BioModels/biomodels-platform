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
 **/

package net.biomodels.jummp.core.model.identifier.generator

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * @short Service for registering and storing model identifier generators.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
class ModelIdentifierGeneratorRegistryService {

    /* the class logger */
    private static final Log log = LogFactory.getLog(this)
    /* semaphore for the log threshold */
    private static final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()
    /* association between a model id generator and its corresponding name */
    Map<String, ModelIdentifierGenerator> registry = [:]

    /**
     * Adds @p generator to the registry under the name @p name.
     * Returns false if any of the arguments is undefined or empty, or if there is already a
     * generator in the registry with name @p name.
     */
    boolean registerGenerator(String name, ModelIdentifierGenerator generator) {
        if (!name || !generator) {
            String e = "Generators must have a name and not be null in order to be registered."
            log.warn(e)
            return false
        }
        if (registry[name]) {
            String e = "A modelIdGenerator named $name already exists."
            log.warn(e)
            return false
        }
        if (IS_DEBUG_ENABLED) {
            log.debug "Registering ${generator.getClass().getName()} as $name"
        }
        registry[name] = generator
        return true
    }

    /**
     * Asks generators in the registry to prepare for providing a new model identifier.
     */
    void updateGenerators() {
        if (IS_DEBUG_ENABLED) {
            log.debug("Triggering an update of all generators: ${registry.inspect()}")
        }
        synchronized(ModelIdentifierGeneratorRegistryService.class) {
            registry.each { g -> g.updateDecorators() }
        }
        if (IS_DEBUG_ENABLED) {
            log.debug("Generators have been updated.")
        }
    }

    /**
     * Returns the generator associated in the registry with @p name, or null if nothing was found.
     */
    ModelIdentifierGenerator findByName(String name) {
        ModelIdentifierGenerator result = registry[name]
        if (IS_DEBUG_ENABLED) {
            log.debug("Searched for a generator named $name and found ${result ?: 'nothing'}.")
        }
        return result
    }
}
