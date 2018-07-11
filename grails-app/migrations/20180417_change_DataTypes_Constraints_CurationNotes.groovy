databaseChangeLog = {

	changeSet(author: "tnguyen (generated)", id: "1523960465682-1") {
		modifyDataType(columnName: "comment", newDataType: "longtext", tableName: "curation_notes")
	}

	changeSet(author: "tnguyen (generated)", id: "1523960465682-2") {
		addNotNullConstraint(columnDataType: "longtext", columnName: "comment", tableName: "curation_notes")
	}

	changeSet(author: "tnguyen (generated)", id: "1523960465682-3") {
		modifyDataType(columnName: "curation_image", newDataType: "mediumblob", tableName: "curation_notes")
	}

	changeSet(author: "tnguyen (generated)", id: "1523960465682-4") {
		addNotNullConstraint(columnDataType: "mediumblob", columnName: "curation_image", tableName: "curation_notes")
	}
}
