package net.biomodels.jummp.core.model.identifier.support

import groovy.sql.Sql
import org.codehaus.groovy.grails.exceptions.DefaultStackTraceFilterer
import org.springframework.beans.factory.annotation.Autowired

import javax.sql.DataSource
import java.sql.SQLException

class PublicationIdGeneratorInitializer extends AbstractModelIdentifierGeneratorInitializer {

    @Autowired
    PublicationIdGeneratorInitializer(DataSource dataSource) {
        super(dataSource)
        queryToRun = 'select max(perennialPublicationIdentifier) as id from model'
        columnToSelect = 'id'
    }

    @Override
    String getLastUsedValue() {
        String result
        try {
            result = executeQuery()
        } catch (SQLException e) {
            def filtered = new DefaultStackTraceFilterer().filter(e)
            throw new IllegalStateException('Unable to extract the latest publication id', filtered)
        }

        result
    }
}
