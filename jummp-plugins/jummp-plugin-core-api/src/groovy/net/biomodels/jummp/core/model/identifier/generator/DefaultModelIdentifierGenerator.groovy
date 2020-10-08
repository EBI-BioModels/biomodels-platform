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
import net.biomodels.jummp.core.model.identifier.decorator.AbstractAppendingDecorator
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
    DefaultModelIdentifierGenerator(String generatorType, SortedSet<? extends OrderedModelIdentifierDecorator> decorators) {
        super(generatorType, decorators)
    }

    DefaultModelIdentifierGenerator(GeneratorDetails details) {
        super(details)
    }

    /**
     * Generates a unique model identifier.
     */
    String generate() {
        synchronized (ModelIdentifier.class) {
            int times = 0
            int maxTries = 5
            while (true) {
                try {
                    ModelIdentifier identifier = new ModelIdentifier()
                    final String MODEL_ID
                    Operations.jedisPool.getResource().withCloseable { Jedis jedis ->
                        Transaction t = jedis.multi()
                        String modelIdLastUsedValue = KeyCollection.getLastUsedIdValueKey(type)
                        String modelIdLastUsedCount = KeyCollection.getLastUsedIdCountKey(type)

                        jedis.watch(modelIdLastUsedValue, modelIdLastUsedCount)
                        String lastUsedIdentifier = Operations.doRedisGet(modelIdLastUsedValue)
                        if (!lastUsedIdentifier) {
                            lastUsedIdentifier = getDefaultIdentifier()
                        }
                        log.debug("IDENTIFIER BASED ON $lastUsedIdentifier")
                        this.update(lastUsedIdentifier)
                        Map<Integer, String> iDParts = new LinkedHashMap<>()
                        def iterator = getDecoratorRegistry().iterator()
                        while (iterator.hasNext()) {
                            def decorator = iterator.next() as AbstractAppendingDecorator
                            iDParts.put(decorator.order, decorator.partition.value)
                            identifier.decorate(decorator, lastUsedIdentifier)
                        }
                        log.debug "Map of the identifier partitions: ${iDParts.dump()}"
                        MODEL_ID = identifier.getCurrentId()
                        if (MODEL_ID) {
                            Operations.doRedisSet(modelIdLastUsedValue, MODEL_ID)
                            t.set(modelIdLastUsedValue, MODEL_ID)
                            // the last decorator is considered as the counter
                            String count = iDParts.values().last()
                            Operations.doRedisSet(modelIdLastUsedCount, count)
                        }
                        List<Object> resp = t.exec()
                        if (resp.size() != 2) {
                            log.debug("Redis transaction cannot commit properly")
                        }
                    }

                    if (IS_DEBUG_ENABLED) {
                        log.debug "Produced a new model identifier $MODEL_ID."
                    }
                    return MODEL_ID
                } catch (Exception e) {
                    log.debug("Trying to generate model identifier $times...")
                    if (++times == maxTries) {
                        log.error("Cannot produce a model identifier")
                        throw e
                    }
                }
            }
        }
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

    String getDefaultIdentifier() {
        String result = ""
        def iterator = getDecoratorRegistry().iterator()
        while (iterator.hasNext()) {
            AbstractAppendingDecorator decorator = iterator.next() as AbstractAppendingDecorator
            result += decorator.initialValue
        }
        return result
    }
}
