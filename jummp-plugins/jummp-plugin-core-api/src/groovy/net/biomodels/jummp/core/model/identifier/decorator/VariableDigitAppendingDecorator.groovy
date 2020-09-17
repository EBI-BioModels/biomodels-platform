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

package net.biomodels.jummp.core.model.identifier.decorator

import grails.util.Holders
import groovy.transform.CompileStatic
import net.biomodels.jummp.utils.redis.KeyCollection
import net.biomodels.jummp.utils.redis.Operations
import net.biomodels.jummp.utils.redis.PublishClient

import net.biomodels.jummp.core.events.ModelIdentifierDecoratorUpdatedEvent
import net.biomodels.jummp.core.model.identifier.ModelIdentifier
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * ModelIdentifierDecorator implementation that adds a numerical suffix to a model id.
 *
 * @author <a href="mailto:mihai.glont@ebi.ac.uk">Mihai Glont</a>
 * @author <a href="mailto:tung.nguyen@ebi.ac.uk">Tung Nguyen</a>
 */
@CompileStatic
class VariableDigitAppendingDecorator extends AbstractAppendingDecorator {
    /* the class logger */
    private static final Log log = LogFactory.getLog(this)
    /* semaphore for the log threshold */
    private static final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()

    Operations redisService

    PublishClient publishClientService = Holders.grailsApplication.mainContext.getBean("publishClientService") as PublishClient

    /**
     * Throws an IllegalArgumentException if @p seed is below 1 or @p width is narrower than
     * the width of @p seed.
     */
    VariableDigitAppendingDecorator(Integer order, long seed, int width)
                throws IllegalArgumentException {
        boolean orderOk = validateOrderValue(order)
        if (!orderOk) {
            log.error "Invalid order $order for $this."
            throw new Exception("Incorrect position at which to insert $this")
        } else {
            ORDER = order
        }
        if (seed < 0) {
            log.error("Cowardly refusing to create a variable digit decorator for seed $seed")
            throw new IllegalArgumentException("Please use strictly positive values in model ids.")
        }
        final int SUFFIX_WIDTH = "$seed".length()
        if (width <= SUFFIX_WIDTH) {
            log.warn("Minimum padding for fixed decorator '$seed' is $SUFFIX_WIDTH, not $width")
            width = SUFFIX_WIDTH
        }
        WIDTH = width
        if (IS_DEBUG_ENABLED) {
            log.debug "Creating ${WIDTH}-digit $this"
        }
    }

    /**
     * Modify model identifier @p modelIdentifier.
     */
    ModelIdentifier decorate(ModelIdentifier modelIdentifier, String lastUsedIdentifier) {
        if (!modelIdentifier) {
            log.warn "Undefined model identifier encountered - decorating a new one instead."
            modelIdentifier = new ModelIdentifier()
        }
        String currentId = modelIdentifier.getCurrentId()
        String next = updateNextValueIfNeeded(lastUsedIdentifier)
        if (IS_DEBUG_ENABLED) {
            log.debug "Decorating $currentId with $next"
        }
        modelIdentifier.append(next)
        partition.value = next
        return modelIdentifier

    }

    /**
     * This method returns false because this implementation appends a different suffix to the
     * supplied model identifier depending on the previous one.
     */
    boolean isFixed() {
        return false
    }

    /**
     * Updates the value that will be appended to the next model identifier if necessary.
     */
    void refresh(final String lastUsedValue) {
        updateNextValueIfNeeded(lastUsedValue)
    }

    /**
     * Resets this decorator's internal counter.
     */
    void reset() {
        // Resets the last counter to 0
        log.debug("Resetting the last counter to 0000")
        publishClientService.publish(KeyCollection.REDIS_CHANNEL_MODEL_ID_LAST_COUNT, "0000")
        redisService.doRedisSet(KeyCollection.getLastUsedIdCountKey(generator.type), "0000")
    }

    private String addLeadingZero(final int newSuffix) {
        String newValue = "${newSuffix}".padLeft(WIDTH, '0')
        newValue
    }

    private String updateNextValueIfNeeded(final String lastUsedValue) {
        final String lastUsedCount = data(lastUsedValue)
        final String lastCount = Operations.doRedisGet(KeyCollection.getLastUsedIdCountKey(generator.type))
        log.debug("Last Count (from Redis): $lastCount")
        // The next counter will be either 1 (when the date segment has been reset and the counter has been reset)
        // or the next value of the last used count (when the date segment was reset)
        Integer nextCount = (lastCount == "0000") ? 1 : (Integer.parseInt(lastUsedCount) + 1)
        final String newValue = addLeadingZero(nextCount)
        log.debug("Last used suffix (extracted from $lastUsedValue): $lastUsedCount <---> Next suffix: $newValue")
        partition.setValue(newValue)
        super.informOfChange(new ModelIdentifierDecoratorUpdatedEvent(this, newValue))
        return newValue
    }
}
