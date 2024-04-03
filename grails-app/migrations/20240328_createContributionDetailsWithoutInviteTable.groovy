databaseChangeLog = {

	changeSet(author: "tnguyen (generated)", id: "1711660886873-1") {
		createTable(tableName: "contribution_details_without_invite") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "contribution_PK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "display_name", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "email", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "orcid", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "revision_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "role_id", type: "bigint") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1711660886873-2") {
		createIndex(indexName: "FK_6sor51ubrsxg6vdt3c3xae0hq", tableName: "contribution_details_without_invite") {
			column(name: "revision_id")
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1711660886873-3") {
		createIndex(indexName: "FK_7icuj7do89ftlb6dgx7tmffnv", tableName: "contribution_details_without_invite") {
			column(name: "role_id")
		}
	}
}
