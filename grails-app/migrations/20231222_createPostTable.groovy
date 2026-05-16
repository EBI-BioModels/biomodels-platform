databaseChangeLog = {

	changeSet(author: "tnguyen (generated)", id: "1703244723008-createPostTable") {
		createTable(tableName: "post") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "postPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "content", type: "varchar(256)") {
				constraints(nullable: "false")
			}

			column(name: "post_by_id", type: "bigint") {
				constraints(nullable: "false")
			}
		}
	}
}
