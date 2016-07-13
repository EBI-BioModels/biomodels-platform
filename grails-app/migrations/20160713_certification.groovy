databaseChangeLog = {

	changeSet(author: "sarala (generated)", id: "1468405552304-1") {
		dropNotNullConstraint(columnDataType: "varchar(255)", columnName: "flag", tableName: "qc_info")
	}

	changeSet(author: "sarala (generated)", id: "1468405552304-3") {
		createIndex(indexName: "FKF074B7DB9D712777", tableName: "revision") {
			column(name: "qc_info_id")
		}
	}

	changeSet(author: "sarala (generated)", id: "1468405552304-2") {
		addForeignKeyConstraint(baseColumnNames: "qc_info_id", baseTableName: "revision", constraintName: "FKF074B7DB9D712777", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "qc_info", referencesUniqueColumn: "false")
	}
}
