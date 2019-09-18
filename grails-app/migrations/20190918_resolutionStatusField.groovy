databaseChangeLog = {

	changeSet(author: "carankalle (generated)", id: "1568804380200-1") {
		createTable(tableName: "auto_generated_category") {
			column(name: "model_identifier", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "category_name", type: "varchar(255)") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "carankalle (generated)", id: "1568804380200-2") {
		createTable(tableName: "p2mmapping") {
			column(name: "representative", type: "varchar(255)") {
				constraints(nullable: "false")
			}

			column(name: "member", type: "varchar(255)") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "carankalle (generated)", id: "1568804380200-3") {
		addColumn(tableName: "resource_reference") {
			column(name: "resolution_status", type: "varchar(255)")
		}
	}

	changeSet(author: "carankalle (generated)", id: "1568804380200-4") {
		addNotNullConstraint(columnDataType: "varchar(1024)", columnName: "body", tableName: "notification")
	}

	changeSet(author: "carankalle (generated)", id: "1568804380200-5") {
		addPrimaryKey(columnNames: "model_identifier, category_name", constraintName: "auto_generatePK", tableName: "auto_generated_category")
	}

	changeSet(author: "carankalle (generated)", id: "1568804380200-6") {
		addPrimaryKey(columnNames: "representative, member", constraintName: "p2mmappingPK", tableName: "p2mmapping")
	}

	changeSet(author: "carankalle (generated)", id: "1568804380200-7") {
		dropIndex(indexName: "idx_ace_mask", tableName: "acl_entry")
	}

	changeSet(author: "carankalle (generated)", id: "1568804380200-8") {
		dropIndex(indexName: "idx_first_published", tableName: "model")
	}

	changeSet(author: "carankalle (generated)", id: "1568804380200-9") {
		dropIndex(indexName: "model_deleted", tableName: "model")
	}

	changeSet(author: "carankalle (generated)", id: "1568804380200-10") {
		dropIndex(indexName: "unique_link", tableName: "publication")
	}

	changeSet(author: "carankalle (generated)", id: "1568804380200-11") {
		dropIndex(indexName: "Accession_Index", tableName: "resource_reference")
	}

	changeSet(author: "carankalle (generated)", id: "1568804380200-12") {
		dropIndex(indexName: "revision_number", tableName: "revision")
	}

	changeSet(author: "carankalle (generated)", id: "1568804380200-13") {
		dropIndex(indexName: "upload_date", tableName: "revision")
	}

	changeSet(author: "carankalle (generated)", id: "1568804380200-14") {
		createIndex(indexName: "FK_o35vm401o3n4qumwwfh5pdd31", tableName: "publication") {
			column(name: "link_provider_id")
		}
	}

	changeSet(author: "carankalle (generated)", id: "1568804380200-15") {
		createIndex(indexName: "content_space_Idx", tableName: "wcm_content") {
			column(name: "space_id")
		}
	}
}
