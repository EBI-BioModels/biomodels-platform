databaseChangeLog = {
    changeSet(author: "tung (generated)", id: "1481933636-3") {
        addColumn(tableName: "element_annotation") {
            column(name: "model_element_type_id", type: "bigint") {
                constraints(nullable: "true")
            }
        }
    }

    /*
    -- run this after updating this column
    changeSet(author: "tung (generated)", id: "1481933636-4") {
        addForeignKeyConstraint(baseColumnNames: "model_element_type_id", baseTableName: "element_annotation",
            constraintName: "FK_ModelElementTypeId", deferrable: "false", initiallyDeferred: "false",
            referencedColumnNames: "id", referencedTableName: "model_element_type", referencesUniqueColumn: "false")
    }*/
}
