databaseChangeLog = {
    changeSet(author: "tnguyen (generated)", id: "1556102664779-1") {
        addColumn(tableName: "tag") {
            column(name: "description", type: "varchar(255)") {
                constraints(nullable: "false")
            }
        }
    }
}
