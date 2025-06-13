databaseChangeLog = {

	changeSet(author: "tnguyen (generated)", id: "1749773561870-1") {
		createTable(tableName: "two_factor_auth") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "two_factor_auPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "issued_date", type: "datetime") {
				constraints(nullable: "false")
			}

			column(name: "otp", type: "varchar(6)") {
				constraints(nullable: "false")
			}

			column(name: "session_id", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "user_id", type: "bigint") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1749773561870-2") {
		createIndex(indexName: "FK_ftt5s3abki7yxb23nyjfvlwk5", tableName: "two_factor_auth") {
			column(name: "user_id")
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1749773561870-3") {
		createIndex(indexName: "otp_uniq_1749773561690", tableName: "two_factor_auth", unique: "true") {
			column(name: "otp")
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1749773561870-4") {
		addForeignKeyConstraint(baseColumnNames: "user_id", baseTableName: "two_factor_auth", constraintName: "FK_ftt5s3abki7yxb23nyjfvlwk5", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "user", referencesUniqueColumn: "false")
	}
}
