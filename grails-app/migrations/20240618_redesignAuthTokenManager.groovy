databaseChangeLog = {

	changeSet(author: "tnguyen (generated)", id: "1718698494-1") {
		addColumn(tableName: "auth_token_manager") {
			column(name: "access_token", type: "varchar(255)") {
				constraints(nullable: "false")
			}
		}
	}

    changeSet(author: "tnguyen (generated)", id: "1718698959221-1") {
        addColumn(tableName: "auth_token_manager") {
            column(name: "user_id", type: "bigint") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "tnguyen (generated)", id: "1718698959221-2") {
        createIndex(indexName: "FK_tdgpk4jctb9pc4acex6feecte", tableName: "auth_token_manager") {
            column(name: "user_id")
        }
    }

    changeSet(author: "tnguyen (generated)", id: "1718698959221-3") {
        addForeignKeyConstraint(baseColumnNames: "user_id", baseTableName: "auth_token_manager",
            constraintName: "FK_tdgpk4jctb9pc4acex6feecte", deferrable: "false", initiallyDeferred: "false",
            referencedColumnNames: "id", referencedTableName: "user", referencesUniqueColumn: "false")
    }
}
