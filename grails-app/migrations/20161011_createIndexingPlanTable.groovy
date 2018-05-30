databaseChangeLog = {
    changeSet(author: "tung (generated)", id: "1476200957-1") {
        createTable(tableName: "indexing_plan") {
            column(autoIncrement: "true", name: "id", type: "bigint") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "indexing_planPK")
            }

            column(name: "version", type: "bigint") {
                constraints(nullable: "false")
            }

            column(name: "model_id", type: "bigint") {
                constraints(nullable: "false")
            }

            column(name: "revision_id", type: "bigint") {
                constraints(nullable: "false")
            }


            column(name: "scheduled_indexing", type: "datetime") {
                constraints(nullable: "false")
            }

            column(name: "status", type: "varchar(255)") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "tung (generated)", id: "1476200957-2") {
        createIndex(indexName: "FK_model_id", tableName: "indexing_plan") {
            column(name: "model_id")
        }
    }

    changeSet(author: "tung (generated)", id: "1476200957-3") {
        createIndex(indexName: "FK_revision_id", tableName: "indexing_plan") {
            column(name: "revision_id")
        }
    }

    changeSet(author: "tung (generated)", id: "1476200957-4") {
        addForeignKeyConstraint(baseColumnNames: "model_id", baseTableName: "indexing_plan",
            constraintName: "FK14762009574", deferrable: "false", initiallyDeferred: "false",
            referencedColumnNames: "id", referencedTableName: "model", referencesUniqueColumn: "false")
    }

    changeSet(author: "tung (generated)", id: "1476200957-5") {
        addForeignKeyConstraint(baseColumnNames: "id", baseTableName: "revision",
            constraintName: "FK14762009575", deferrable: "false", initiallyDeferred: "false",
            referencedColumnNames: "revision_id", referencedTableName: "indexing_plan", referencesUniqueColumn: "false")
    }
}
