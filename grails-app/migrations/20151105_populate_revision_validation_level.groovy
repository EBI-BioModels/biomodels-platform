databaseChangeLog = {
    changeSet(author: "Mihai Glont", id: "populate-revision-validation-level") {
        grailsChange {
            change {
                def revisionTable = sql.dataSet("revision")
                revisionTable.rows().each { r ->
                    final boolean SHOULD_MODIFY = !(r.validation_level)
                    if (SHOULD_MODIFY) {
                        sql.executeUpdate "update revision set validation_level = 'APPROVE' where id = ${r.id}"
                    }
                }
            }
        }
    }
}
