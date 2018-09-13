package net.biomodels.jummp.core.model.identifier.support

import groovy.sql.Sql
import org.codehaus.groovy.grails.exceptions.DefaultStackTraceFilterer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.sql.DataSource
import java.sql.SQLException

class SubmissionIdGeneratorInitializer implements ModelIdentifierGeneratorInitializer {
    final Logger log = LoggerFactory.getLogger(getClass())

    DataSource dataSource

    @Override
    String getLastUsedValue() {
        if (!dataSource) {
            throw new IllegalStateException("""Called outside of an application context, \
please initialise the dataSource bean prior to invoking this method""")
        }
        Sql sql = new Sql(dataSource)
        String result = null
        try {
            def row = sql.firstRow("""\
select submission_id as id from model
where id = (
    select model_id from revision
    where upload_date = (
        select max(upload_date) from revision where revision_number = 1
    )
    LIMIT 1
)
""")
            result = row?.id
            // TODO ADD THE FOLLOWING DB INDICES
            /*ALTER TABLE revision add index `revisionNumber` (`revision_number`);
            ALTER TABLE revision add index `uploadDate` (`upload_date`);*/
        } catch (SQLException e) {
            def filtered = new DefaultStackTraceFilterer().filter(e)
            throw new IllegalStateException('Unable to extract the latest submission id', filtered)
        } finally {
            sql.close()
        }

        log.debug("${toString()} Most recent submission id is $result")
        result
    }
}
