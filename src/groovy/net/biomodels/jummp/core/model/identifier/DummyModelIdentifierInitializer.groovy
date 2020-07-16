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

package net.biomodels.jummp.core.model.identifier

import groovy.transform.CompileStatic
import net.biomodels.jummp.core.model.identifier.support.ModelIdentifierGeneratorInitializer

/**
 * ModelIdentifierGeneratorInitializer implementation that can be told what last value to report.
 *
 * Useful in unit and integration tests when it is not desirable or feasible to perform a database
 * lookup in order to decide what generator seed value to return.
 *
 * @see ModelIdentifierGeneratorInitializer
 */
@CompileStatic
class DummyModelIdentifierInitializer implements ModelIdentifierGeneratorInitializer {
    String lastUsedValue

    DummyModelIdentifierInitializer(String lastUsedValue) {
        this.lastUsedValue = lastUsedValue
    }

    @Override
    String getRedisKeyForLastUsedValue() {
        return null
    }
}
