databaseChangeLog = {

	changeSet(author: "raza (generated)", id: "1434640184197-1") {
		createTable(tableName: "curation_notes") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "curation_notePK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "comment", type: "text") {
				constraints(nullable: "false")
			}

			column(name: "curation_image", type: "mediumblob")

			column(name: "date_added", type: "datetime") {
				constraints(nullable: "false")
			}

			column(name: "last_modified", type: "datetime") {
				constraints(nullable: "false")
			}

			column(name: "last_modifier_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "model_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "submitter_id", type: "bigint") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "raza (generated)", id: "1434640184197-26") {
		createIndex(indexName: "FK21B1B6B7735B95CF", tableName: "curation_notes") {
			column(name: "last_modifier_id")
		}
	}

	changeSet(author: "raza (generated)", id: "1434640184197-27") {
		createIndex(indexName: "FK21B1B6B7ADCECDF", tableName: "curation_notes") {
			column(name: "model_id")
		}
	}

	changeSet(author: "raza (generated)", id: "1434640184197-28") {
		createIndex(indexName: "FK21B1B6B7BABE6A86", tableName: "curation_notes") {
			column(name: "submitter_id")
		}
	}

	changeSet(author: "raza (generated)", id: "1434640184197-18") {
		addForeignKeyConstraint(baseColumnNames: "last_modifier_id", baseTableName: "curation_notes", constraintName: "FK21B1B6B7735B95CF", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "user", referencesUniqueColumn: "false")
	}

	changeSet(author: "raza (generated)", id: "1434640184197-19") {
		addForeignKeyConstraint(baseColumnNames: "model_id", baseTableName: "curation_notes", constraintName: "FK21B1B6B7ADCECDF", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "model", referencesUniqueColumn: "false")
	}

	changeSet(author: "raza (generated)", id: "1434640184197-20") {
		addForeignKeyConstraint(baseColumnNames: "submitter_id", baseTableName: "curation_notes", constraintName: "FK21B1B6B7BABE6A86", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "user", referencesUniqueColumn: "false")
	}
	
	
	changeSet(author: "raza (generated)", id: "1434639714393-2") {
		createTable(tableName: "model_of_the_month") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "model_of_the_PK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "authors", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "last_updated", type: "datetime") {
				constraints(nullable: "false")
			}

			column(name: "publication_date", type: "datetime") {
				constraints(nullable: "false")
			}

			column(name: "title", type: "varchar(255)") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "raza (generated)", id: "1434639714393-3") {
		createTable(tableName: "model_of_the_month_model") {
			column(name: "model_of_the_month_models_id", type: "bigint") {
			    constraints(nullable: "false")
			}

			column(name: "model_id", type: "bigint") {
			    constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "raza (generated)", id: "1434639714393-29") {
		createIndex(indexName: "FKC7F7344AADCECDF", tableName: "model_of_the_month_model") {
			column(name: "model_id")
		}
	}

	changeSet(author: "raza (generated)", id: "1434639714393-30") {
		createIndex(indexName: "FKC7F7344AF4826511", tableName: "model_of_the_month_model") {
			column(name: "model_of_the_month_models_id")
		}
	}

	changeSet(author: "raza (generated)", id: "1434639714393-21") {
		addForeignKeyConstraint(baseColumnNames: "model_id", baseTableName: "model_of_the_month_model", constraintName: "FKC7F7344AADCECDF", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "model", referencesUniqueColumn: "false")
	}

	changeSet(author: "raza (generated)", id: "1434639714393-22") {
		addForeignKeyConstraint(baseColumnNames: "model_of_the_month_models_id", baseTableName: "model_of_the_month_model", constraintName: "FKC7F7344AF4826511", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "model_of_the_month", referencesUniqueColumn: "false")
	}

}
