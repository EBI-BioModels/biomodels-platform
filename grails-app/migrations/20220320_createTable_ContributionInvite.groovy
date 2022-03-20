databaseChangeLog = {
    changeSet(author: "Tung Nguyen", id: "1647817188-1") {
        createTable(tableName: "contribution_invite") {
            column(name: "whoSent", type: "bigint") {
                constraints(nullable: "false")
            }

            column(name: "email", type: "varchar(128)") {
                constraints(nullable: "false")
            }

            column(name: "firstname", type: "varchar(128)") {
                constraints(nullable: "false")
            }

            column(name: "lastname", type: "varchar(256)") {
                constraints(nullable: "false")
            }

            column(name: "dateSent", type: "datetime") {
                constraints(nullable: "false")
            }

            column(name: "dateAccepted", type: "datetime") {
                constraints(nullable: "true")
            }
            // value: PENDING, CANCELLED, ACCEPTED
            column(name: "status", type: "varchar(32)", defaultValue: "PENDING", value: "PENDING") {
                constraints(nullable: "true")
            }
        }
    }

    changeSet(author: "Tung Nguyen", id: "1647817188-2") {
        addPrimaryKey(columnNames: "whoSent, email",
            constraintName: "contribution_invite_cPK", tableName: "contribution_invite")
    }
}
