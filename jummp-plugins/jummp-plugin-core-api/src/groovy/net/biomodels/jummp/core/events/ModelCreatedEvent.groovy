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





package net.biomodels.jummp.core.events

import net.biomodels.jummp.core.model.ModelTransportCommand

/**
 * @short Event triggered when a new Model is uploaded.
 *
 * This event is triggered by the ModelService when a user successfully uploaded
 * a new Model. Interested parties might listen to this event through an ApplicationListener
 * and use the information provided. It is not possible to alter the Model or the uploaded
 * files in any way. Instead of the Model a ModelTransportCommand is included in the event.
 * @author Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
class ModelCreatedEvent extends ModelOperationEvent {
    /**
     * The newly created model.
     */
    final ModelTransportCommand model
    /**
     * The actual files which were uploaded.
     */
    final List<File> files

    ModelCreatedEvent(Object source, final ModelTransportCommand model, final List<File> files) {
        super(source)
        this.model = model
        this.files = files
    }
}
