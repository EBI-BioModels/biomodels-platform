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

import net.biomodels.jummp.core.events.ModelIdentifierDecoratorUpdatedEvent
import net.biomodels.jummp.core.model.identifier.decorator.OrderedModelIdentifierDecorator

/**
 * @short Interface defining the contract for components that wish to produce model identifiers.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
interface ModelIdentifierGenerator {
    /**
     * Generates a model identifier.
     */
    String generate()
    /**
     * Asks its variable decorators to prepare the values they will use for the next identifier.
     */
    void update(final String lastUsedValue)

    /**
     * Callback for ModelIdentifierDecorator updates.
     *
     * @param event a description of the changes (the decorator that changed and its new value)
     * @see {@link net.biomodels.jummp.core.model.identifier.decorator.ModelIdentifierDecorator#informOfChange(net.biomodels.jummp.core.events.ModelIdentifierDecoratorUpdatedEvent)}
     */
    void respondTo(ModelIdentifierDecoratorUpdatedEvent event)

    /**
     * Returns the regular expression pattern that matches identifiers created by this generator.
     *
     * @return the String representation of regex pattern or null if this generator did not have
     *         its regex computed during instantiation.
     * @see net.biomodels.jummp.core.model.identifier.support.ModelIdentifierPartitionRegexFactory
     */
    String getRegex()

    String getType()

    /**
     * Returns the generator's {@link net.biomodels.jummp.core.model.identifier.decorator.OrderedModelIdentifierDecorator decorators}.
     *
     * @return the model id decorators in the order in which they were defined.
     */
    SortedSet<? extends OrderedModelIdentifierDecorator> getDecoratorRegistry()
}
