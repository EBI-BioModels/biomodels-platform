import liquibase.statement.core.UpdateStatement
databaseChangeLog = {

	changeSet(author: "carankalle (generated)", id: "1569840589580-1") {
		addColumn(tableName: "resource_reference") {
			column(name: "resolution_status", type: "varchar(255)") {
				constraints(nullable: "false")
			}
		}
	}
	changeSet(author: "carankalle (generated)", id: "1569840589580-8") {
		createIndex(indexName: "resolution_status_index", tableName: "resource_reference") {
			column(name: "resolution_status")
		}
	}
    changeSet(author: "carankalle", id: "update the resolution_status with default value") {
        grailsChange {
            change {
                def statements = []

                statements << new UpdateStatement("", 'resource_reference')
                    .addNewColumnValue('resolution_status', "UNRESOLVED")
                    .setWhereClause(" resolution_status is null or resolution_status=''")

                sqlStatements(statements)

                confirm 'Initializing default value'
            }
        }
    }

}
