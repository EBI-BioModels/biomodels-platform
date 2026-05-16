databaseChangeLog = {
	changeSet(author: "tnguyen (generated)", id: "1734195404197") {
		addColumn(tableName: "contribution_details_without_invite") {
			column(name: "affiliation", type: "varchar(512)")
		}
	}
}
