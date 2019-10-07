import liquibase.statement.core.UpdateStatement
databaseChangeLog = {

    changeSet(author: "carankalle", id: "populate the resolution_status with RESOLVED value") {
        grailsChange {
            change {
                def statements = []

                statements << new UpdateStatement("", 'resource_reference')
                    .addNewColumnValue('resolution_status', "RESOLVED")
                    .setWhereClause(" name is not null and resolution_status!='RESOLVED'")

                sqlStatements(statements)

                confirm 'Initializing resolution_status with RESOLVED value where name is not null'
            }
        }
    }

}
