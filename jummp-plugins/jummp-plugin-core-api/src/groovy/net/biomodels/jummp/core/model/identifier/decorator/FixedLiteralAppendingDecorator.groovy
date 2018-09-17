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
import net.biomodels.jummp.core.model.identifier.ModelIdentifier
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * @short ModelIdentifierDecorator implementation that adds a fixed literal suffix to all model ids.
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
@CompileStatic
class FixedLiteralAppendingDecorator extends AbstractAppendingDecorator {
    /* the suffix used to decorate model identifiers. */
    final String SUFFIX
    /* the class logger */
    private static final Log log = LogFactory.getLog(this)
    /* semaphore for the log threshold */
    private static final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()

    protected FixedLiteralAppendingDecorator(int order) throws IllegalArgumentException {
        boolean orderOk = validateOrderValue(order)
        if (!orderOk) {
            log.error "Invalid order $order for $this."
            throw new IllegalArgumentException("Incorrect position at which to insert $this")
        } else {
            ORDER = order
        }
    }

    /**
     * Don't pass an empty String or null to avoid an IllegalArgumentException.
     */
    FixedLiteralAppendingDecorator(int order, String suffix)
                throws IllegalArgumentException {
        this(order)
        if (!suffix) {
            log.error("Cowardly refusing to create a string decorator for suffix $suffix")
            throw new IllegalArgumentException("Please use non-empty string suffixes in model ids.")
        }
        if (IS_DEBUG_ENABLED) {
            log.debug "Creating $this with suffix $suffix"
        }
        nextValue.set(suffix)
        SUFFIX = suffix
    }

    /**
     * Modify model identifier @p modelIdentifier.
     */
    ModelIdentifier decorate(ModelIdentifier modelIdentifier) {
        if (modelIdentifier) {
            String currentId = modelIdentifier.getCurrentId()
            final String next = nextValue.get()
            if (IS_DEBUG_ENABLED) {
                log.debug "Decorating $currentId with $next"
            }
            return modelIdentifier.append(next)
        } else {
            log.warn "Undefined model identifier encountered - decorating a new one instead."
            ModelIdentifier result = new ModelIdentifier()
            return result.append(nextValue.get())
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
    void refresh() {
    }
}

