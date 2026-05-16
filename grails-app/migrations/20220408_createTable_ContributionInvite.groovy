databaseChangeLog = {
    changeSet(author: "Tung Nguyen", id: "1649437890534-1") {
        createTable(tableName: "contribution_invite") {
            column(name: "inviter_id", type: "bigint") {
                constraints(nullable: "false")
            }

            column(name: "invitee_email", type: "varchar(255)") {
                constraints(nullable: "false")
            }

            column(name: "date_completed", type: "datetime")

            column(name: "date_sent", type: "datetime") {
                constraints(nullable: "false")
            }

            column(name: "reference", type: "varchar(255)") {
                constraints(nullable: "false")
            }

            column(name: "revision_id", type: "bigint") {
                constraints(nullable: "false")
            }

            column(name: "role_id", type: "bigint") {
                constraints(nullable: "false")
            }

            column(name: "state", type: "varchar(255)") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "tnguyen (generated)", id: "1649437890534-2") {
        addPrimaryKey(columnNames: "inviter_id, invitee_email, revision_id, role_id",
            constraintName: "contribution_PK", tableName: "contribution_invite")
    }
}
