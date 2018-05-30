databaseChangeLog = {
	changeSet(author: "tnguyen (generated)", id: "1505386625932-1") {
		createTable(tableName: "feedback") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "feedbackPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "comment", type: "varchar(1024)")

			column(name: "email", type: "varchar(128)")

			column(name: "star", type: "tinyint") {
				constraints(nullable: "false")
			}
		}
	}
}
