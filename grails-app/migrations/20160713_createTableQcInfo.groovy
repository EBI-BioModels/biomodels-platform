databaseChangeLog = {
    changeSet(author: "tung (generated)", id: "1468405552304-1") {
		createTable(tableName: "qc_info") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "comment", type: "varchar(1000)") {
				constraints(nullable: "false")
			}

			column(name: "flag", type: "varchar(255)") {
				constraints(nullable: "true")
			}
		}
	}
}
