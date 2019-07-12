databaseChangeLog = {

    changeSet(author: "tnguyen (generated)", id: "1562620430180-1") {
        createTable(tableName: "auto_generated_category") {
            column(name: "model_identifier", type: "varchar(255)") {
                constraints(nullable: "false")
            }

            column(name: "category_name", type: "varchar(255)") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "tnguyen (generated)", id: "1562620430180-3") {
        addPrimaryKey(columnNames: "model_identifier, category_name", constraintName: "auto_generatePK", tableName: "auto_generated_category")
    }
}
