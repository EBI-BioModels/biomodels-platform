databaseChangeLog = {
	changeSet(author: "tnguyen (generated)", id: "1506095900876-42") {
		createIndex(indexName: "unique_email", tableName: "feedback", unique: "true") {
			column(name: "email")
			column(name: "star")
		}
	}
}
