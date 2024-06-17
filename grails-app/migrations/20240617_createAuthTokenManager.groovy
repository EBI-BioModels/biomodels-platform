/**
 * Adding expired_date into the auth_token table doesn't bring back an expected outcome.
 * Using another table to store all other attributes to mange the usages of access tokens.
 */
databaseChangeLog = {

	changeSet(author: "tnguyen (generated)", id: "1718660361274-1") {
		createTable(tableName: "auth_token_manager") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "auth_token_maPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "auth_token_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "created_date", type: "datetime") {
				constraints(nullable: "false")
			}

			column(name: "expired_date", type: "datetime") {
				constraints(nullable: "false")
			}

			column(name: "hit_count", type: "bigint") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1718660361274-2") {
		createIndex(indexName: "FK_t9qlsu1rdoxhrnr6bx4qbopv7", tableName: "auth_token_manager") {
			column(name: "auth_token_id")
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1718660361274-3") {
		addForeignKeyConstraint(baseColumnNames: "auth_token_id", baseTableName: "auth_token_manager", constraintName: "FK_t9qlsu1rdoxhrnr6bx4qbopv7", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "auth_token", referencesUniqueColumn: "false")
	}

    changeSet(author: "tnguyen (generated)", id: "1718660361274-4") {
        dropColumn(columnName: "expired_date", tableName: "auth_token")
    }
}
