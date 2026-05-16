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

    changeSet(author: "tnguyen (generated)", id: "1718660361274-4") {
        dropColumn(columnName: "expired_date", tableName: "auth_token")
    }
}
