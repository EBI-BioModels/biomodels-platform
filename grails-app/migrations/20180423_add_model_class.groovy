databaseChangeLog = {

	changeSet(author: "tvu (generated)", id: "1524487109103-1") {
		createTable(tableName: "model_class") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "model_classPK")
			}

			column(name: "version", type: "bigint")
			column(name: "class", type: "varchar(15)")
			column(name: "create_by", type: "bigint")
			column(name: "create_time", type: "datetime")
			column(name: "model_id", type: "bigint")
			column(name: "status", type: "integer")
			column(name: "update_time", type: "datetime")
		}
	}

	changeSet(author: "tvu (generated)", id: "1524487109103-72") {
		createIndex(indexName: "FK_4uwd0f5bkp4en2r02g6lwr0cv", tableName: "model_class") {
			column(name: "create_by")
		}
	}

	changeSet(author: "tvu (generated)", id: "1524487109103-73") {
		createIndex(indexName: "FK_o8u5yobwtt1sldukqi03svodo", tableName: "model_class") {
			column(name: "model_id")
		}
	}

	changeSet(author: "tvu (generated)", id: "1524487109103-25") {
		addForeignKeyConstraint(baseColumnNames: "create_by", baseTableName: "model_class", constraintName: "FK_4uwd0f5bkp4en2r02g6lwr0cv", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "user", referencesUniqueColumn: "false")
	}

	changeSet(author: "tvu (generated)", id: "1524487109103-26") {
		addForeignKeyConstraint(baseColumnNames: "model_id", baseTableName: "model_class", constraintName: "FK_o8u5yobwtt1sldukqi03svodo", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "model", referencesUniqueColumn: "false")
	}
}
