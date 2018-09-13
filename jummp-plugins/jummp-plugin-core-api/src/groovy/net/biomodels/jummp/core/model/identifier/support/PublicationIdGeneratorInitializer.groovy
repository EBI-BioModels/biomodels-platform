package net.biomodels.jummp.core.model.identifier.support

import org.springframework.beans.factory.annotation.Autowired

import javax.sql.DataSource
import java.sql.SQLException

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
