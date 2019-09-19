databaseChangeLog = {

	changeSet(author: "carankalle (generated)", id: "1568907619115-1") {
		addColumn(tableName: "resource_reference") {
			column(name: "resolution_status", type: "varchar(255)")
		}
	}

	changeSet(author: "carankalle (generated)", id: "1568907619115-6") {
		createIndex(indexName: "resolution_status_index", tableName: "resource_reference") {
			column(name: "resolution_status")
		}
	}

}
