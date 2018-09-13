package net.biomodels.jummp.core.model.identifier.support

import groovy.sql.Sql
import javax.sql.DataSource
import java.sql.SQLException

abstract class AbstractModelIdentifierGeneratorInitializer implements ModelIdentifierGeneratorInitializer {
    DataSource dataSource
    String queryToRun
    String columnToSelect

    protected AbstractModelIdentifierGeneratorInitializer() {}

    AbstractModelIdentifierGeneratorInitializer(DataSource dataSource) {
        this.dataSource = dataSource
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
        } finally {
            sql.close()
        }

        result
    }
}
