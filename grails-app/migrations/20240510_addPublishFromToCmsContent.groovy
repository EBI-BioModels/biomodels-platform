databaseChangeLog = {

	changeSet(author: "tnguyen (generated)", id: "1715347103579-1") {
		addColumn(tableName: "cms_content") {
			column(name: "published_from", type: "datetime")
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1715347103579-2") {
		addColumn(tableName: "cms_content") {
			column(name: "published_to", type: "datetime")
		}
	}
}
