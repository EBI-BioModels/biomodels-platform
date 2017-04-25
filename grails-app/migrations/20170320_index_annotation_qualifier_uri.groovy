databaseChangeLog = {
	changeSet(author: "mglont (generated)", id: "1490035399366-1") {
		createIndex(indexName: "uri_uniq_1490035399023", tableName: "qualifier", unique: "true") {
			column(name: "uri")
		}
	}

	changeSet(author: "mglont (generated)", id: "1490035399366-2") {
		createIndex(indexName: "uri_uniq_1490035399023", tableName: "resource_reference", unique: "true") {
			column(name: "uri")
		}
	}
}
