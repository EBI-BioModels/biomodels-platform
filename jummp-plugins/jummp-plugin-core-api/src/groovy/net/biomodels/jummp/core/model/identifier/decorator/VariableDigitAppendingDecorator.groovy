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

import java.util.concurrent.atomic.AtomicLong
import net.biomodels.jummp.core.events.ModelIdentifierDecoratorUpdatedEvent
import net.biomodels.jummp.core.model.identifier.ModelIdentifier
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * @short ModelIdentifierDecorator implementation that adds a numerical suffix to a model id.
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
public class VariableDigitAppendingDecorator extends AbstractAppendingDecorator {
    /* the width of the suffix used to decorate model identifiers. */
    final Integer WIDTH
    /* the number used in the last model id, without padding. */
    private final AtomicLong lastUsedSuffix = new AtomicLong(-1)
    /* the value to use in the next id, without padding. Effectively, the dual of nextValue */
    private final AtomicLong nextSuffix = new AtomicLong()
    /* the class logger */
    private static final Log log = LogFactory.getLog(this)
    /* semaphore for the log threshold */
    private static final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()

    /**
     * Throws an IllegalArgumentException if @p seed is below 1 or @p width is narrower than
     * the width of @p seed.
     */
    public VariableDigitAppendingDecorator(Integer order, long seed, int width)
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
        updateNextValueIfNeeded()
        if (modelIdentifier) {
            String currentId = modelIdentifier.getCurrentId()
            final String next = nextValue.get()
            if (IS_DEBUG_ENABLED) {
                log.debug "Decorating $currentId with $next."
            }
            modelIdentifier.append(next)
            lastUsedSuffix.set(nextSuffix.get())
            return modelIdentifier
        } else {
            log.warn "Undefined model identifier encountered - decorating a new one instead."
            ModelIdentifier result = new ModelIdentifier()
            result.append(nextValue.get())
            lastUsedSuffix.set(nextSuffix.get())
            return result
        }
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

    private void updateNextValueIfNeeded() {
        if (lastUsedSuffix.get() == nextSuffix.get()) {
            long newSuffix = nextSuffix.incrementAndGet()
            String newValue = "${newSuffix}".padLeft(WIDTH, '0')
            nextValue.set(newValue)
            if (IS_DEBUG_ENABLED) {
                log.debug "Incremented nextValue to ${newValue}"
            }
            super.publishEvent(new ModelIdentifierDecoratorUpdatedEvent(this, newValue))
        }
    }
}
