import net.biomodels.jummp.core.model.identifier.generator.NullModelIdentifierGenerator

databaseChangeLog = {
    changeSet(author: "Mihai Glont", id: "populate-submission-id-20140624") {
        grailsChange {
            change {
                def sig = ctx.getBean("submissionIdGenerator")
                def modelTable = sql.dataSet("model")
                modelTable.rows().each { m ->
                    final boolean GENERATE_IDENTIFIER = !(m.submission_id)
                    if (GENERATE_IDENTIFIER) {
                        String sId = sig.generate()
                        sql.executeUpdate "update model set submission_id = $sId where id = ${m.id}"
                    }
                }
            }
        }
    }
}
