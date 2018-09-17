/**
 * Copyright (C) 2010-2018 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
import net.biomodels.jummp.core.model.identifier.ModelIdentifier
import net.biomodels.jummp.core.events.DateModelIdentifierDecoratorUpdatedEvent
import net.biomodels.jummp.core.events.ModelIdentifierDecoratorUpdatedEvent
import net.biomodels.jummp.core.model.identifier.decorator.AbstractAppendingDecorator
import net.biomodels.jummp.core.model.identifier.decorator.OrderedModelIdentifierDecorator
import net.biomodels.jummp.core.model.identifier.decorator.VariableDigitAppendingDecorator
import net.biomodels.jummp.core.model.identifier.support.GeneratorDetails
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * @short Abstract implementation for producing model identifiers.
 *
 * This class also implements
 * {@link ModelIdentifierGenerator#respondTo(net.biomodels.jummp.core.events.ModelIdentifierDecoratorUpdatedEvent)}
 * in order to respond to events issued by ModelIdentifierDecorator implementations.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
abstract class AbstractModelIdentifierGenerator implements ModelIdentifierGenerator {
    /** the class logger. */
    private static final Log log = LogFactory.getLog(this)
    private static final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()
    /**
     * The registry of decorators which an implementation may use to generate model identifiers.
     */
    SortedSet<? extends OrderedModelIdentifierDecorator> decoratorRegistry
    /**
     * The regular expression corresponding to identifiers this generator instance creates.
     */
    String regex

    /**
     * Default constructor
     */
    protected AbstractModelIdentifierGenerator() {
        this((SortedSet) null)
    }

    /**
     * Constructs a generator instance with the given decorators.
     *
     * @param decorators an ordered set of model id decorators.
     */
    protected AbstractModelIdentifierGenerator(
            SortedSet<? extends OrderedModelIdentifierDecorator> decorators) {
        doSetDecoratorRegistry(decorators)
        regex = null
    }

    /**
     *  Constructs a generator instance from the provided {@code GeneratorDetails}.
     *
     * @param details the set of decorators and the regex belonging to this instance.
     */
    protected AbstractModelIdentifierGenerator(GeneratorDetails details) {
        /*
         * this line causes errors with Groovy 2.4.5 if the class has @CompileStatic
         * NoSuchMethodError: GeneratorDetails.getDecorators()Ljava/util/TreeSet
         * No idea why, but I left @CompileStatic on the often-used methods of this class
         */
        this(details?.decorators)
        regex = details?.regex
    }

    abstract String generate()

    abstract void update()

    @CompileStatic
    SortedSet<? extends OrderedModelIdentifierDecorator> getDecoratorRegistry() {
        if (null == decoratorRegistry || decoratorRegistry.isEmpty()) {
            final String msg = "At least one decorator is needed to make model identifiers for $this."
            log.error(msg)
            throw new IllegalStateException(msg)
        }

        return decoratorRegistry
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    void setDecoratorRegistry(SortedSet<? extends OrderedModelIdentifierDecorator> registry) {
        doSetDecoratorRegistry(registry)
    }

    /**
     * @inherit
     */
    @CompileStatic
    void respondTo(ModelIdentifierDecoratorUpdatedEvent event) {
        if (IS_DEBUG_ENABLED) {
            log.debug "Processing event ${event.inspect()}"
        }
        synchronized(ModelIdentifier.class) {
            if (event instanceof DateModelIdentifierDecoratorUpdatedEvent) {
                // finds the first variable digit decorator and reset its value.
                VariableDigitAppendingDecorator d = decoratorRegistry.find {
                    it instanceof VariableDigitAppendingDecorator
                } as VariableDigitAppendingDecorator
                if (d) {
                    d.reset()
                    if (IS_DEBUG_ENABLED) {
                        log.debug "Attribute 'nextValue' of $d has been reset to ${d.nextValue.get()}."
                    }
                }
            }
        }
    }

    @CompileStatic
    private void doSetDecoratorRegistry(
            SortedSet<? extends OrderedModelIdentifierDecorator> decorators) {
        if (!decorators) return

        decoratorRegistry = Collections.unmodifiableSortedSet(decorators)
        // tell the decorators who to notify in case of changes
        Iterator<? extends OrderedModelIdentifierDecorator> i = decoratorRegistry.iterator()
        while (i.hasNext()) {
            //noinspection ChangeToOperator
            OrderedModelIdentifierDecorator nextDecorator = i.next()
            if (nextDecorator instanceof AbstractAppendingDecorator) {
                nextDecorator.generator = this
            }
        }
    }
}
