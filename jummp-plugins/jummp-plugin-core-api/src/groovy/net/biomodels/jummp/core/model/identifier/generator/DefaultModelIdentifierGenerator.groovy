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

import groovy.transform.CompileStatic
import net.biomodels.jummp.core.model.identifier.decorator.OrderedModelIdentifierDecorator
import net.biomodels.jummp.core.model.identifier.ModelIdentifier
import net.biomodels.jummp.core.model.identifier.support.GeneratorDetails
import net.biomodels.jummp.utils.redis.Operations
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * @short Default ModelIdentifierGenerator implementation for producing model identifiers.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
@CompileStatic
class DefaultModelIdentifierGenerator extends AbstractModelIdentifierGenerator {
    /* the class logger */
    private static final Log log = LogFactory.getLog(this)
    /* semaphore for the log threshold */
    private static final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()

    @SuppressWarnings("GroovyUnusedDeclaration")
    DefaultModelIdentifierGenerator() {
    }

    /**
     * Initialises the decorators that should be used by this class instance.
     */
    DefaultModelIdentifierGenerator(SortedSet<? extends OrderedModelIdentifierDecorator> decorators) {
        super(decorators)
    }

    DefaultModelIdentifierGenerator(GeneratorDetails details) {
        super(details)
    }

    /**
     * Generates a unique model identifier.
     */
    String generate() {
        ModelIdentifier identifier = new ModelIdentifier()
        final String MODEL_ID
        def iterator = getDecoratorRegistry().iterator()
        synchronized(ModelIdentifier.class) {
            while (iterator.hasNext()) {
                def decorator = iterator.next()
                identifier.decorate(decorator)
            }
            MODEL_ID = identifier.getCurrentId()
        }
        if (IS_DEBUG_ENABLED) {
            log.debug "Produced a new model identifier $MODEL_ID."
        }
        Operations.doRedisSet('model-id-last-used-value', MODEL_ID)
        return MODEL_ID
    }

    /**
     * Asks decorators in DECORATOR_REGISTRY to prepare new values for the next identifier.
     */
    void update() {
        def iterator = getDecoratorRegistry().iterator()
        while (iterator.hasNext()) {
            def decorator = iterator.next()
            decorator.isFixed() ?: decorator.refresh()
        }
    }
}
