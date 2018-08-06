databaseChangeLog = {
    changeSet(author: "tung (generated)", id: "1481933636-1") {
        createTable(tableName: "model_element_type") {
            column(autoIncrement: "true", name: "id", type: "bigint") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "PK_modelElementType")
            }

            column(name: "version", type: "bigint") {
                constraints(nullable: "false")
            }

            column(name: "model_format_id", type: "bigint") {
                constraints(nullable: "false")
            }

            column(name: "name", type: "varchar(128)") {
                constraints(nullable: "false")
            }
        }
    }
    changeSet(author: "tung (generated)", id: "1481933636-2") {
        addForeignKeyConstraint(baseColumnNames: "model_format_id", baseTableName: "model_element_type",
            constraintName: "FK_ModelFormatId", deferrable: "false", initiallyDeferred: "false",
            referencedColumnNames: "id", referencedTableName: "model_format", referencesUniqueColumn: "false")
    }
}
