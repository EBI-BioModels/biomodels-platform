package net.biomodels.jummp.core.model.identifier.support

import groovy.sql.Sql
import org.codehaus.groovy.grails.exceptions.DefaultStackTraceFilterer

import javax.sql.DataSource
import java.sql.SQLException

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
