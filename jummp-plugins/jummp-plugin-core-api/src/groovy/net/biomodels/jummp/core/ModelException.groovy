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





package net.biomodels.jummp.core

import net.biomodels.jummp.core.model.ModelTransportCommand

/**
 * @short Exception thrown when manipulating a Model fails.
 *
 * This exception should be thrown by all service methods manipulating Models or their revisions.
 * It indicates that the operation on the Model failed.
 * @author Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 */
class ModelException extends JummpException implements Serializable {
    private static final long serialVersionUID = 1L
    /**
     * The Model which failed to be manipulated.
     */
    private ModelTransportCommand model

    ModelException(final String message) {
        super(message)
    }

    ModelException(ModelTransportCommand model) {
        this(model, "Failed to manipulate Model ${model?.id}".toString())
    }

    ModelException(ModelTransportCommand model, String message) {
        super(message)
        this.model = model
    }

    ModelException(ModelTransportCommand model, Throwable cause) {
        this(model, "Failed to manipulate Model ${model?.id}".toString(), cause)
    }

    ModelException(ModelTransportCommand model, String message, Throwable cause) {
        super(message, cause)
        this.model = model
    }

    ModelTransportCommand getModel() {
        return model
    }
}
