databaseChangeLog = {
	changeSet(author: "tnguyen (generated)", id: "add-resource-reference-accession-index") {
		createIndex(indexName: "idx_accession", tableName: "resource_reference") {
			column(name: "accession")
		}
	}
}
