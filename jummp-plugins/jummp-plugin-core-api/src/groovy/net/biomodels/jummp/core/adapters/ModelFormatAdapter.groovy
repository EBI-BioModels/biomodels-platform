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

package net.biomodels.jummp.core.adapters
import net.biomodels.jummp.model.ModelFormat
import net.biomodels.jummp.core.model.ModelFormatTransportCommand
/**
 * @short Adapter class for the Model Format domain class
 *
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 */
class ModelFormatAdapter {
    ModelFormat format

    ModelFormatTransportCommand toCommandObject() {
        Map argsMap = [id: format.id,
                       identifier: format.identifier,
                       name: format.name,
                       formatVersion: format.formatVersion]
        return new ModelFormatTransportCommand(argsMap)
    }

    boolean ignoreCheckingVersion() {
        ignoreVersion(format.identifier)
    }

    static boolean ignoreCheckingVersion(final ModelFormatTransportCommand command) {
        ignoreVersion(command.identifier)
    }

    private static boolean ignoreVersion(final String identifier) {
        boolean retVal = false
        switch (identifier.toLowerCase()) {
            case "python":
                retVal = true
                break
            case "c_cpp":
                retVal = true
                break
            case "r":
                retVal = true
                break
            case "java":
                retVal = true
                break
            case "mathematica":
                retVal = true
                break
            default:
                retVal = false
                break
        }
        return retVal
    }
}
