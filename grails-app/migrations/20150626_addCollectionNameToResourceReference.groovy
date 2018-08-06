databaseChangeLog = {

	changeSet(author: "mglont (generated)", id: "1435276170713-1") {
		addColumn(tableName: "resource_reference") {
			column(name: "collection_name", type: "varchar(255)") {
				constraints(nullable: "true")
			}
		}
	}
}
