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

import groovy.transform.CompileStatic
import net.biomodels.jummp.utils.redis.Operations
import net.biomodels.jummp.utils.redis.PublishClient

import java.util.concurrent.atomic.AtomicLong
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
    /* the width of the suffix used to decorate model identifiers. */
    Integer WIDTH
    /* the number used in the last model id, without padding. */
    private final AtomicLong lastUsedSuffix = new AtomicLong(-1)
    /* the value to use in the next id, without padding. Effectively, the dual of nextValue */
    private final AtomicLong nextSuffix = new AtomicLong()
    /* the class logger */
    private static final Log log = LogFactory.getLog(this)
    /* semaphore for the log threshold */
    private static final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()
    /* REDIS key/channel string for model identifier's last counter */
    static final String REDIS_MODEL_ID_LAST_COUNT = "model-id-last-count"

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
        nextSuffix.compareAndSet(0, seed)
        nextValue.compareAndSet(null, "$seed".padLeft(width, '0'))
        WIDTH = width
        if (IS_DEBUG_ENABLED) {
            log.debug "Creating ${WIDTH}-digit $this"
        }
    }

    /**
     * Modify model identifier @p modelIdentifier.
     */
    ModelIdentifier decorate(ModelIdentifier modelIdentifier) {
        final String lastCount = Operations.doRedisGet(REDIS_MODEL_ID_LAST_COUNT)
        Integer nextCount = !lastCount ? 1 : (Integer.parseInt(lastCount) + 1)
        log.debug "Last used suffix: $lastUsedSuffix <---> Next suffix: $nextCount"
        PublishClient.publish(REDIS_MODEL_ID_LAST_COUNT, "$nextCount".toString())
        /**
         * publishing the next count as a message, then subscribers can see the message
         * The subscriber which updates the last counter is probably out of the synchronisation
         * process applied for the entire ModelIdentifier.class. The statement below aims to update
         * the last counter straightaway.
         * {@see DefaultModelIdentifierGenerator.generate()}
         */
        Operations.doRedisSet(REDIS_MODEL_ID_LAST_COUNT, "$nextCount".toString())
        final String next = addLeadingZero(nextCount)
        if (!modelIdentifier) {
            log.warn "Undefined model identifier encountered - decorating a new one instead."
            modelIdentifier = new ModelIdentifier()
        }
        String currentId = modelIdentifier.getCurrentId()
        if (IS_DEBUG_ENABLED) {
            log.debug "Decorating $currentId with $next."
        }
        modelIdentifier.append(next)
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
    void refresh() {
        updateNextValueIfNeeded()
    }

    /**
     * Resets this decorator's internal counter.
     */
    void reset() {
        // Resets the last counter to 0
        PublishClient.publish(REDIS_MODEL_ID_LAST_COUNT, "0")
    }

    private String addLeadingZero(final int newSuffix) {
        String newValue = "${newSuffix}".padLeft(WIDTH, '0')
        newValue
    }

    private void updateNextValueIfNeeded() {
        if (lastUsedSuffix.get() == nextSuffix.get()) {
            long newSuffix = nextSuffix.incrementAndGet()
            String newValue = "${newSuffix}".padLeft(WIDTH, '0')
            nextValue.set(newValue)
            if (IS_DEBUG_ENABLED) {
                log.debug "Incremented nextValue to ${newValue}"
            }
            super.informOfChange(new ModelIdentifierDecoratorUpdatedEvent(this, newValue))
        }
    }
}
