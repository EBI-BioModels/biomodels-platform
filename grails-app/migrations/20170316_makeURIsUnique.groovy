databaseChangeLog = {
	changeSet(author: "mglont (generated)", id: "1489671098598-7") {
		createIndex(indexName: "qualifier_uri_unique", tableName: "qualifier", unique: "true") {
			column(name: "uri")
		}
	}

	changeSet(author: "mglont (generated)", id: "1489671098598-8") {
		createIndex(indexName: "xref_uri_unique", tableName: "resource_reference", unique: "true") {
			column(name: "uri")
		}
    }
}
