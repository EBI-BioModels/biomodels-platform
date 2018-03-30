databaseChangeLog = {

	changeSet(author: "tvu (generated)", id: "1522163825441-1") {
		addColumn(tableName: "revision") {
			column(name: "curation_state", type: "varchar(31)", defaultValue: "NON_CURATED", value: "NON_CURATED") {
				constraints(nullable: "false")
			}
		}
	}

    changeSet(author: "tvu (generated)", id: "1522163825442-1") {
        grailsChange {
            change {
                query = "UPDATE revision, model SET revision.curation_state = 'CURATED' " +
                    "WHERE revision.model_id = model.id AND model.perennialPublicationIdentifier IS NOT NULL "
                sql.executeUpdate(query)
            }
        }
    }

    changeSet(author: "tvu (generated)", id: "1522163825443-1") {
        grailsChange {
            change {
                query = "UPDATE revision SET revision.state = 'UNPUBLISHED' " +
                    "WHERE revision.state = 'UNDER_CURATION' "
                sql.executeUpdate(query)
            }
        }
    }
}
databaseChangeLog = {

	changeSet(author: "tvu (generated)", id: "1522163825441-1") {
		addColumn(tableName: "revision") {
			column(name: "curation_state", type: "varchar(31)", defaultValue: "NON_CURATED", value: "NON_CURATED") {
				constraints(nullable: "false")
			}
		}
	}

    changeSet(author: "tvu (generated)", id: "1522163825442-1") {
        grailsChange {
            change {
                query = "UPDATE revision, model SET revision.curation_state = 'CURATED' " +
                    "WHERE revision.model_id = model.id AND model.perennialPublicationIdentifier IS NOT NULL "
                sql.executeUpdate(query)
            }
        }
    }

    changeSet(author: "tvu (generated)", id: "1522163825443-1") {
        grailsChange {
            change {
                query = "UPDATE revision SET revision.curation_state = 'UNPUBLISHED' " +
                    "WHERE revision.state = 'UNDER_CURATION' "
                sql.executeUpdate(query)
            }
        }
    }
}
