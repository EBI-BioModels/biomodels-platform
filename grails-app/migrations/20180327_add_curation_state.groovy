databaseChangeLog = {

	changeSet(author: "tvu (generated)", id: "1522163825441-1") {
		addColumn(tableName: "revision") {
			column(name: "curation_state", type: "varchar(31)", defaultValue: "NON_CURATED", value: "NON_CURATED") {
				constraints(nullable: "false")
			}
		}
	}
}
