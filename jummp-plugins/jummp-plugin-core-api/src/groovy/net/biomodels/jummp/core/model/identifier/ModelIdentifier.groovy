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

package net.biomodels.jummp.core.model.identifier

import net.biomodels.jummp.core.model.identifier.decorator.ModelIdentifierDecorator
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * @short Simple POGO facilitating the generation of a model identifier.
 *
 * Instances of this class are manipulated by concrete implementations of the
 * ModelIdentifierDecorator interface.
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
public class ModelIdentifier {
    /* the class logger */
    private static final Log log = LogFactory.getLog(this)
    /* semaphore for the log threshold */
    private static final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()
    /* the value of this model identifier */
    private final StringBuffer id

    ModelIdentifier() {
        id = new StringBuffer()
    }

    ModelIdentifier decorate(ModelIdentifierDecorator decorator) {
        if (!decorator) {
            log.warn "Undefined decorator asked to append $this"
            return this
        }
        if (IS_DEBUG_ENABLED) {
            log.debug "Asking $decorator to decorate $this."
        }
        decorator.decorate(this)
    }

    ModelIdentifier append(String snippet) {
        if (!snippet) {
            log.warn "Ignoring request to add null/empty snippet to $this"
        } else {
            if (IS_DEBUG_ENABLED) {
                log.debug "Appending $snippet to $this"
            }
            id.append(snippet)
        }
        return this
    }

    @Override
    String toString() {
        return "ModelIdentifier $id"
    }

    public String getCurrentId() {
        return id.toString()
    }

    private void setId(StringBuffer ignored) {
    }
}
