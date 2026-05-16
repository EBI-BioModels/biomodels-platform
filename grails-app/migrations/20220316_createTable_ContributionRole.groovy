databaseChangeLog = {
	changeSet(author: "Tung Nguyen", id: "create table Contribution Role") {
		createTable(tableName: "contribution_role") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "contribution_PK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "description", type: "varchar(1500)")

			column(name: "name", type: "varchar(255)") {
				constraints(nullable: "false")
			}
		}
	}
}
