databaseChangeLog = {
	changeSet(author: "mglont (generated)", id: "add-statement-index") {
		createIndex(indexName: "Subject_Predicate_Object_Index", tableName: "statement") {
			column(name: "object_id")

			column(name: "qualifier_id")

			column(name: "subject_id")
		}
	}
}
