databaseChangeLog = {

	changeSet(author: "sarala (generated)", id: "1444642187148-1") {
		addColumn(tableName: "revision") {
			column(name: "validation_level", type: "varchar(255)") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "sarala (generated)", id: "1444642187148-2") {
		addColumn(tableName: "revision") {
			column(name: "validation_report", type: "text")
		}
	}
}
