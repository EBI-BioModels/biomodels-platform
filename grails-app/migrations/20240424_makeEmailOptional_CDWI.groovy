databaseChangeLog = {
	changeSet(author: "tnguyen (generated)", id: "1713955049879-1") {
		dropNotNullConstraint(columnDataType: "varchar(255)", columnName: "email",
            tableName: "contribution_details_without_invite")
	}
}
