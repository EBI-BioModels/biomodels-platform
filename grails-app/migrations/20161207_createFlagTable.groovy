databaseChangeLog = {
    changeSet(author: "tung (generated)", id: "29882366350-1") {
        createTable(tableName: "flag") {
            column(autoIncrement: "true", name: "id", type: "bigint") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "flagPK")
            }

            column(name: "version", type: "bigint") {
                constraints(nullable: "false")
            }

            column(name: "label", type: "varchar(255)") {
                constraints(nullable: "false")
            }

            column(name: "description", type: "varchar(2048)") {
                constraints(nullable: "false")
            }

            column(name: "icon", type: "blob") {
                constraints(nullable: "false")
            }
        }
    }
}
