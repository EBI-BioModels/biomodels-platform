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

import groovy.sql.Sql
import org.codehaus.groovy.grails.exceptions.DefaultStackTraceFilterer

import javax.sql.DataSource
import java.sql.SQLException

/**
 * Common superclass for {@code ModelIdentifierGeneratorInitializer} implementations.
 *
 * Contains support methods that are commonly needed by concrete subclasses.
 *
 * @see net.biomodels.jummp.core.model.identifier.support.ModelIdentifierGeneratorInitializer
 */
abstract class AbstractModelIdentifierGeneratorInitializer implements ModelIdentifierGeneratorInitializer {
    DataSource dataSource
    String queryToRun
    String columnToSelect

    protected AbstractModelIdentifierGeneratorInitializer() {}

    AbstractModelIdentifierGeneratorInitializer(DataSource dataSource, String queryToRun,
            String columnToSelect) {
        this.dataSource     = dataSource
        this.queryToRun     = queryToRun
        this.columnToSelect = columnToSelect
    }

    def executeQuery() throws SQLException {
        if (!dataSource) {
            throw new IllegalStateException("""Called outside of an application context, \
please initialise the dataSource bean prior to invoking this method""")
        }

        Sql sql = new Sql(dataSource)
        String result = null
        try {
            def row = sql.firstRow(queryToRun)
            result = row?."$columnToSelect"
        } catch (SQLException e) {
            def filtered = new DefaultStackTraceFilterer().filter(e)
            throw filtered
        } finally {
            sql.close()
        }

        result
    }
}
