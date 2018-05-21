databaseChangeLog = {

	changeSet(author: "tvu (generated)", id: "1526563584160-1") {
		createTable(tableName: "model_class") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "model_classPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "class", type: "varchar(63)") {
				constraints(nullable: "false")
			}

			column(name: "created_by", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "created_date", type: "datetime") {
				constraints(nullable: "false")
			}

			column(name: "model_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "status", type: "integer") {
				constraints(nullable: "false")
			}

			column(name: "updated_by", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "updated_date", type: "datetime") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "tvu (generated)", id: "1526563584160-54") {
		createIndex(indexName: "FK_8syufqgqbw4gi81cemj9tpmt5", tableName: "model_class") {
			column(name: "updated_by")
		}
	}

	changeSet(author: "tvu (generated)", id: "1526563584160-55") {
		createIndex(indexName: "FK_o8u5yobwtt1sldukqi03svodo", tableName: "model_class") {
			column(name: "model_id")
		}
	}

	changeSet(author: "tvu (generated)", id: "1526563584160-56") {
		createIndex(indexName: "FK_sqhs6s0tutircsu77h1hab39w", tableName: "model_class") {
			column(name: "created_by")
		}
	}

	changeSet(author: "tvu (generated)", id: "1526563584160-12") {
		addForeignKeyConstraint(baseColumnNames: "created_by", baseTableName: "model_class",
            constraintName: "FK_sqhs6s0tutircsu77h1hab39w", deferrable: "false", initiallyDeferred: "false",
            referencedColumnNames: "id", referencedTableName: "user", referencesUniqueColumn: "false")
	}

	changeSet(author: "tvu (generated)", id: "1526563584160-13") {
		addForeignKeyConstraint(baseColumnNames: "model_id", baseTableName: "model_class",
            constraintName: "FK_o8u5yobwtt1sldukqi03svodo", deferrable: "false", initiallyDeferred: "false",
            referencedColumnNames: "id", referencedTableName: "model", referencesUniqueColumn: "false")
	}

	changeSet(author: "tvu (generated)", id: "1526563584160-14") {
		addForeignKeyConstraint(baseColumnNames: "updated_by", baseTableName: "model_class",
            constraintName: "FK_8syufqgqbw4gi81cemj9tpmt5", deferrable: "false", initiallyDeferred: "false",
            referencedColumnNames: "id", referencedTableName: "user", referencesUniqueColumn: "false")
	}
}
