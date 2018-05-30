databaseChangeLog = {

	changeSet(author: "mglont (generated)", id: "1485960435382-1") {
		dropNotNullConstraint(columnDataType: "varchar(255)", columnName: "journal", tableName: "publication")
	}

	changeSet(author: "mglont (generated)", id: "1485960435382-2") {
		dropNotNullConstraint(columnDataType: "varchar(5000)", columnName: "synopsis", tableName: "publication")
	}

	changeSet(author: "mglont (generated)", id: "1485960435382-3") {
		dropNotNullConstraint(columnDataType: "varchar(1000)", columnName: "affiliation", tableName: "publication")
	}
}
