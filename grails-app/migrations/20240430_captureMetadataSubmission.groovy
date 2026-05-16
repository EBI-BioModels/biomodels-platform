databaseChangeLog = {
	changeSet(author: "tnguyen (generated)", id: "1714481110228-1") {
		addColumn(tableName: "model") {
			column(name: "is_metadata_submission", type: "bit")
		}

        sql("update model set is_metadata_submission = false")
	}
}
