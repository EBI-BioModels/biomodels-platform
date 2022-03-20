databaseChangeLog = {
    changeSet(author: "Tung Nguyen", id: "create table Contribution Details") {
        createTable(tableName: "contribution_details") {
            column(name: "user_id", type: "bigint") {
                constraints(nullable: "false")
            }

            column(name: "revision_id", type: "bigint") {
                constraints(nullable: "false")
            }

            column(name: "contribution_role_id", type: "bigint") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "Tung Nguyen", id: "create the composite primary key for Contribution Details") {
        addPrimaryKey(columnNames: "user_id, revision_id, contribution_role_id",
            constraintName: "contribution_details_cPK", tableName: "contribution_details")
    }
}
