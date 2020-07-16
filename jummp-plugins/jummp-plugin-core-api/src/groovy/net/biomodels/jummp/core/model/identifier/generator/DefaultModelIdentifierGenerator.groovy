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

import grails.util.Holders
import groovy.transform.CompileStatic
import net.biomodels.jummp.core.model.identifier.decorator.OrderedModelIdentifierDecorator
import net.biomodels.jummp.core.model.identifier.ModelIdentifier
import net.biomodels.jummp.core.model.identifier.support.GeneratorDetails
import net.biomodels.jummp.utils.redis.KeyCollection
import net.biomodels.jummp.utils.redis.Operations
import net.biomodels.jummp.utils.redis.PublishClient
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import redis.clients.jedis.Jedis
import redis.clients.jedis.Transaction

/**
 * @short Default ModelIdentifierGenerator implementation for producing model identifiers.
 *
 * @author <a href="mailto:tung.nguyen@ebi.ac.uk">Tung Nguyen</a>
 * @author <a href="mailto:mihai.glont@ebi.ac.uk">Mihai Glont</a> */
@CompileStatic
class DefaultModelIdentifierGenerator extends AbstractModelIdentifierGenerator {
    /* the class logger */
    private static final Log log = LogFactory.getLog(this)
    /* semaphore for the log threshold */
    private static final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()

    PublishClient publishClientService = Holders.grailsApplication.mainContext.getBean("publishClientService") as PublishClient

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
        synchronized (ModelIdentifier.class) {
            Operations.jedisPool.getResource().withCloseable { Jedis jedis ->
                Transaction t = jedis.multi()
                String generatorType = typeOfIdentifierGenerator()
                String modelIdLastUsedValue
                String modelIdLastUsedCount
                if (generatorType == "BIOMD") {
                    modelIdLastUsedValue = KeyCollection.PUBLICATION_ID_LAST_USED_VALUE
                    modelIdLastUsedCount = KeyCollection.PUBLICATION_ID_LAST_USED_COUNT
                } else {
                    modelIdLastUsedValue = KeyCollection.MODEL_ID_LAST_USED_VALUE
                    modelIdLastUsedCount = KeyCollection.MODEL_ID_LAST_COUNT
                }
                jedis.watch(modelIdLastUsedValue, modelIdLastUsedCount)
                String lastUsedIdentifier = Operations.doRedisGet(modelIdLastUsedValue)
                log.debug("IDENTIFIER BASED ON $lastUsedIdentifier")
                this.update(lastUsedIdentifier)
                def iterator = getDecoratorRegistry().iterator()
                while (iterator.hasNext()) {
                    def decorator = iterator.next()
                    identifier.decorate(decorator, lastUsedIdentifier)
                }
                MODEL_ID = identifier.getCurrentId()
                if (MODEL_ID) {
                    Operations.doRedisSet(modelIdLastUsedValue, MODEL_ID)
                    t.set(modelIdLastUsedValue, MODEL_ID)
                    String count
                    // TODO: refactor this using ModelIdentifierPartitionManager or Decorator
                    // using the variable appending decorator to guess the last used count
                    if (generatorType == "BIOMD") {
                        count = MODEL_ID[5..14]
                    } else {
                        count = MODEL_ID[11..14]
                    }
                    Operations.doRedisSet(modelIdLastUsedCount, count)
                }
                List<Object> resp = t.exec()
                if (resp.size() != 2) {
                    log.debug("Redis transaction cannot commit properly")
                }
            }
        }
        if (IS_DEBUG_ENABLED) {
            log.debug "Produced a new model identifier $MODEL_ID."
        }
        return MODEL_ID
    }

    /**
     * Asks decorators in DECORATOR_REGISTRY to prepare new values for the next identifier.
     */
    void update(final String lastUsedValue) {
        def iterator = getDecoratorRegistry().iterator()
        while (iterator.hasNext()) {
            def decorator = iterator.next()
            decorator.isFixed() ?: decorator.refresh(lastUsedValue)
        }
    }
}
