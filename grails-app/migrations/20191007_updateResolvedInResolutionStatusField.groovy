import liquibase.statement.core.UpdateStatement
databaseChangeLog = {

    changeSet(author: "carankalle (customized)", id: "1570800300.545341") {
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
