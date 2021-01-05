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

import net.biomodels.jummp.core.model.identifier.ModelIdentifierUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

import javax.sql.DataSource
import java.sql.SQLException

/**
 * Initialiser for submission id generators.
 *
 * @see net.biomodels.jummp.core.model.identifier.support.ModelIdentifierGeneratorInitializer
 */
class SubmissionIdGeneratorInitializer extends AbstractModelIdentifierGeneratorInitializer {
    private static final String query = """select submission_id from model where id = (
        select model_id
        from revision
        where upload_date = (select max(upload_date) from revision where revision_number = 1)
        limit 1
    )"""
    private static final String column = "submission_id"
    final Logger log = LoggerFactory.getLogger(getClass())

    @Autowired
    SubmissionIdGeneratorInitializer(DataSource dataSource) {
        super(dataSource, query, column, ModelIdentifierUtils.DEFAULT_GENERATOR_TYPE)
    }

    @Override
    String getLastUsedValue() {
        synchronized (this) {
            String result
            try {
                result = executeQuery()
            } catch (SQLException e) {
                throw new IllegalStateException('Unable to extract the latest submission id', e)
            }

            log.debug("Most recent submission id is $result")
            result
        }
    }
}
