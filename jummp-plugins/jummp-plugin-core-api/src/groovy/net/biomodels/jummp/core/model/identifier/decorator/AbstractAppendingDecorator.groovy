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
import net.biomodels.jummp.core.model.identifier.generator.ModelIdentifierGenerator

import java.util.concurrent.atomic.AtomicReference
import net.biomodels.jummp.core.model.identifier.ModelIdentifier
import net.biomodels.jummp.core.events.ModelIdentifierDecoratorUpdatedEvent
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * @short Abstract ModelIdentifierDecorator implementation defining the natural order.
 *
 * This class also provides a default implementation for publishing events, as required by
 * ApplicationEventPublisher.
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
@CompileStatic
abstract class AbstractAppendingDecorator implements OrderedModelIdentifierDecorator {
    /**
     * Reference to the value used to decorate the next model identifier.
     */
    final AtomicReference<String> nextValue = new AtomicReference<>()
    /**
     * The class logger
     */
    private final Log log = LogFactory.getLog(getClass())
    /**
     * The position of the decorator in the queue of a ModelIdentifierGenerator.
     */
    protected volatile int ORDER
    /**
     * The generator to which this decorator belongs.
     */
    ModelIdentifierGenerator generator

    abstract ModelIdentifier decorate(ModelIdentifier modelIdentifier, String lastUsedIdentifier)

    abstract boolean isFixed()

    abstract void refresh(final String lastUsedValue)

    /**
     * Informs the generator of a change to this decorator's value.
     */
    void informOfChange(ModelIdentifierDecoratorUpdatedEvent evt) {
        if (null != generator) {
            if (log.isDebugEnabled()) {
                log.debug("Asking ${generator.properties} to respond to ${evt.properties}")
            }
            generator.respondTo(evt)
        }
    }

    /**
     * Defines the natural order for instances of OrderedModelIdentifierDecorator implementations.
     *
     * This base implementation arranges decorators in ascending order of their {@code ORDER} and
     * assumes that @p other is also an instance of {@link AbstractAppendingDecorator}.
     *
     * Concrete subclasses may choose to relax these constraints.
     */
    @Override
    int compareTo(OrderedModelIdentifierDecorator other) {
        this.ORDER <=> ((AbstractAppendingDecorator) other).ORDER
    }

    @Override
    String toString() {
        "${this.getClass().name} order: $ORDER nextValue: ${nextValue.get()}"
    }

    /**
     * Helper method that checks whether the supplied @p order falls within the range [0, 2^15-1).
     */
    protected boolean validateOrderValue(int order) {
        return order >= 0 && order < Integer.MAX_VALUE
    }

    protected void setOrder(int order) {
        ORDER = order
    }
}

