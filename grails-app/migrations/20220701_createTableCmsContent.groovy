databaseChangeLog = {

	changeSet(author: "tnguyen (generated)", id: "1656678864789-1") {
		createTable(tableName: "cms_content") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "cms_contentPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "aliasuri", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "content", type: "longtext")

			column(name: "created_by_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "created_on", type: "datetime") {
				constraints(nullable: "false")
			}

			column(name: "description", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "last_changed_by_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "last_changed_on", type: "datetime") {
				constraints(nullable: "false")
			}

			column(name: "parent_id", type: "bigint")

			column(name: "title", type: "varchar(255)") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1656678864789-33") {
		createIndex(indexName: "FK_5xm871ofbdbbhf7b7qpdf1p0k", tableName: "cms_content") {
			column(name: "parent_id")
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1656678864789-34") {
		createIndex(indexName: "FK_6xys7a5dbn8c835drc7hdldyj", tableName: "cms_content") {
			column(name: "last_changed_by_id")
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1656678864789-35") {
		createIndex(indexName: "FK_mqjcne3u24cyk7dqda91ev9kg", tableName: "cms_content") {
			column(name: "created_by_id")
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1656678864789-36") {
		createIndex(indexName: "aliasuri_uniq_1656678864252", tableName: "cms_content", unique: "true") {
			column(name: "aliasuri")
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1656678864789-37") {
		createIndex(indexName: "content_aliasURI_Idx", tableName: "cms_content") {
			column(name: "aliasuri")
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1656678864789-38") {
		createIndex(indexName: "title_uniq_1656678864253", tableName: "cms_content", unique: "true") {
			column(name: "title")
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1656678864789-3") {
		addForeignKeyConstraint(baseColumnNames: "created_by_id", baseTableName: "cms_content",
            constraintName: "FK_mqjcne3u24cyk7dqda91ev9kg", deferrable: "false", initiallyDeferred: "false",
            referencedColumnNames: "id", referencedTableName: "user", referencesUniqueColumn: "false")
	}

	changeSet(author: "tnguyen (generated)", id: "1656678864789-4") {
		addForeignKeyConstraint(baseColumnNames: "last_changed_by_id", baseTableName: "cms_content",
            constraintName: "FK_6xys7a5dbn8c835drc7hdldyj", deferrable: "false", initiallyDeferred: "false",
            referencedColumnNames: "id", referencedTableName: "user", referencesUniqueColumn: "false")
	}

	changeSet(author: "tnguyen (generated)", id: "1656678864789-5") {
		addForeignKeyConstraint(baseColumnNames: "parent_id", baseTableName: "cms_content",
            constraintName: "FK_5xm871ofbdbbhf7b7qpdf1p0k", deferrable: "false", initiallyDeferred: "false",
            referencedColumnNames: "id", referencedTableName: "cms_content", referencesUniqueColumn: "false")
	}
}
