databaseChangeLog = {

	changeSet(author: "tnguyen (generated)", id: "1492076192446-1") {
		addColumn(tableName: "model_of_the_month") {
			column(name: "preview_image", type: "blob")
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1492076192446-2") {
		addColumn(tableName: "model_of_the_month") {
			column(name: "short_description", type: "varchar(1024)")
		}
	}
}
