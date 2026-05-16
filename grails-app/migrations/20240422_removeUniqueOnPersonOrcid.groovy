databaseChangeLog = {
	changeSet(author: "tnguyen (generated)", id: "1713809389046-17") {
		dropIndex(indexName: "UK_a4i5gt8woyulsvd6uq34qo409", tableName: "person")
	}

	changeSet(author: "tnguyen (generated)", id: "1713809389046-18") {
		dropIndex(indexName: "person_name", tableName: "person")
	}
}
