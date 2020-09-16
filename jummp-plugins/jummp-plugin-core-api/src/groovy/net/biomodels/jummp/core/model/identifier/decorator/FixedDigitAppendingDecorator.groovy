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

import net.biomodels.jummp.core.model.identifier.ModelIdentifier
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * @short ModelIdentifierDecorator implementation adding a constant numerical suffix to all model ids.
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
class FixedDigitAppendingDecorator extends AbstractAppendingDecorator {
    /* the class logger */
    private static final Log log = LogFactory.getLog(this)
    /* semaphore for the log threshold */
    private static final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()

    protected FixedDigitAppendingDecorator() {
    }

    /**
     * Don't pass a suffix below 1 to avoid an IllegalArgumentException.
     */
    public FixedDigitAppendingDecorator(Integer order, long suffix, int width)
                throws IllegalArgumentException {
        boolean orderOk = validateOrderValue(order)
        if (!orderOk) {
            log.error "Invalid order $order for $this."
            throw new Exception("Incorrect position at which to insert $this")
        } else {
            ORDER = order
        }
        if (suffix < 1) {
            log.error("Cowardly refusing to create a fixed digit decorator for suffix $suffix")
            throw new IllegalArgumentException("Please use strictly positive values in model ids.")
        }
        final int SUFFIX_WIDTH = "$suffix".length()
        if (width <= SUFFIX_WIDTH) {
            log.warn("Minimum padding for fixed decorator '$suffix' is $SUFFIX_WIDTH, not $width")
            width = SUFFIX_WIDTH
        }
        nextValue.compareAndSet(null, "$suffix".padLeft(width, '0'))
        WIDTH = width
        if (IS_DEBUG_ENABLED) {
            log.debug "Creating ${WIDTH}-digit $this"
        }
    }

    /**
     * Modify model identifier @p modelIdentifier.
     */
    ModelIdentifier decorate(ModelIdentifier modelIdentifier, String lastUsedIdentifier) {
        String next
        if (modelIdentifier) {
            String currentId = modelIdentifier.getCurrentId()
            next = nextValue.get()
            if (IS_DEBUG_ENABLED) {
                log.debug "Decorating $currentId with $next"
            }
            modelIdentifier.append(next)
            partition.value = next
            return modelIdentifier
        } else {
            log.warn "Undefined model identifier encountered - decorating a new one instead."
            ModelIdentifier result = new ModelIdentifier()
            next = nextValue.get()
            result.append(next)
            partition.value = next
            return result
        }
    }

    /**
     * This decorator always appends the same suffix to all model identifiers.
     */
    boolean isFixed() {
        return true
    }

    /**
     * Nothing to do.
     */
    void refresh(final String lastUsedValue) {}
}

