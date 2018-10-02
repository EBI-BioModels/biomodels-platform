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

package net.biomodels.jummp.core.model.identifier.support

import org.springframework.beans.factory.annotation.Autowired

import javax.sql.DataSource
import java.sql.SQLException

/**
 * Initialiser for publication id generators.
 *
 * @see net.biomodels.jummp.core.model.identifier.support.ModelIdentifierGeneratorInitializer
 */
class PublicationIdGeneratorInitializer extends AbstractModelIdentifierGeneratorInitializer {
    private static final String query = 'select max(perennialPublicationIdentifier) as id from model'
    private static final String column = 'id'

    @Autowired
    PublicationIdGeneratorInitializer(DataSource dataSource) {
        super(dataSource, query, column)
    }

    @Override
    String getLastUsedValue() {
        String result
        try {
            result = executeQuery()
        } catch (SQLException e) {
            throw new IllegalStateException('Unable to extract the latest publication id', e)
        }

        result
    }
}
