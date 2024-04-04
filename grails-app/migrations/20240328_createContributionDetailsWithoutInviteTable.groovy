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

			column(name: "orcid", type: "varchar(255)")

			column(name: "revision_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "role_id", type: "bigint") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1711660886873-2") {
		createIndex(indexName: "FK_6sor51ubrsxg6vdt3c3xae0hq",
            tableName: "contribution_details_without_invite") {
			column(name: "revision_id")
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1711660886873-3") {
		createIndex(indexName: "FK_7icuj7do89ftlb6dgx7tmffnv",
            tableName: "contribution_details_without_invite") {
			column(name: "role_id")
		}
	}

    changeSet(author: "tnguyen (generated)", id: "1712215210313-1") {
        createIndex(indexName: "orcid_uniq_1712215209477",
            tableName: "contribution_details_without_invite", unique: "true") {
            column(name: "orcid")
        }
    }

    changeSet(author: "tnguyen (generated)", id: "1712215210313-7") {
        addForeignKeyConstraint(baseColumnNames: "revision_id",
            baseTableName: "contribution_details_without_invite",
            constraintName: "FK_6sor51ubrsxg6vdt3c3xae0hq",
            deferrable: "false", initiallyDeferred: "false",
            referencedColumnNames: "id", referencedTableName: "revision",
            referencesUniqueColumn: "false")
    }

    changeSet(author: "tnguyen (generated)", id: "1712215210313-8") {
        addForeignKeyConstraint(baseColumnNames: "role_id",
            baseTableName: "contribution_details_without_invite",
            constraintName: "FK_7icuj7do89ftlb6dgx7tmffnv",
            deferrable: "false", initiallyDeferred: "false",
            referencedColumnNames: "id",
            referencedTableName: "contribution_role",
            referencesUniqueColumn: "false")
    }
}
