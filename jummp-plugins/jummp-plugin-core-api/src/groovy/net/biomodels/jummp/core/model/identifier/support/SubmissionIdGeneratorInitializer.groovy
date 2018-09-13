package net.biomodels.jummp.core.model.identifier.support

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.sql.DataSource
import java.sql.SQLException

class SubmissionIdGeneratorInitializer extends AbstractModelIdentifierGeneratorInitializer {
    private static final String query = """select submission_id from model where id = (
        select model_id 
        from revision
        where upload_date = (select max(upload_date) from revision where revision_number = 1)
        limit 1
    )"""
    private static final String column = "submission_id"
    final Logger log = LoggerFactory.getLogger(getClass())

    SubmissionIdGeneratorInitializer(DataSource dataSource) {
        super(dataSource, query, column)
    }

    @Override
    String getLastUsedValue() {
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
