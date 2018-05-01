databaseChangeLog = {

	changeSet(author: "tnguyen (generated)", id: "1523282861598-1") {
		addColumn(tableName: "curation_notes") {
			column(name: "internal_comment", type: "longtext")
		}
	}
}
