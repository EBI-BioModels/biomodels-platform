/**
 * Copyright (C) 2010-2016 EMBL-European Bioinformatics Institute (EMBL-EBI),
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





package net.biomodels.jummp.core.model

import net.biomodels.jummp.model.Flag

/**
 * Convenience class for adding customised methods to the Flag class.
 *
 * Relies on Groovy categories, a form of runtime meta-programming.
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */

@Category(Flag)
class FlagCategory {
    /**
     * Provides a lightweight command object that can be used outside Jummp's core.
     *
     * @return the FlagTransportCommand representation of a Flag object.
     */
    public FlagTransportCommand toCommandObject() {
        String label = this.label
        String description = this.description
        byte[] icon = this.icon
        return new FlagTransportCommand(label: label, description: description, icon: icon)
    }
}
