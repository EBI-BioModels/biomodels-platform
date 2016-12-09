databaseChangeLog = {
    changeSet(author: "tung (generated)", id: "29882366350-2") {
        createTable(tableName: "model_flag") {
            column(autoIncrement: "true", name: "id", type: "bigint") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "modelFlagPK")
            }

            column(name: "version", type: "bigint") {
                constraints(nullable: "false")
            }

            column(name: "model_id", type: "bigint") {
                constraints(nullable: "false")
            }

            column(name: "flag_id", type: "bigint") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "tung (generated)", id: "29882366350-3") {
        addForeignKeyConstraint(baseColumnNames: "model_id", baseTableName: "model_flag",
            constraintName: "FK_ModelID", deferrable: "false", initiallyDeferred: "false",
            referencedColumnNames: "id", referencedTableName: "model", referencesUniqueColumn: "false")
    }

    changeSet(author: "tung (generated)", id: "29882366350-4") {
        addForeignKeyConstraint(baseColumnNames: "flag_id", baseTableName: "model_flag",
            constraintName: "FK_FlagID", deferrable: "false", initiallyDeferred: "false",
            referencedColumnNames: "id", referencedTableName: "flag", referencesUniqueColumn: "false")
    }
}
