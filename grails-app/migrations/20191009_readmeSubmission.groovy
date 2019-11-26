databaseChangeLog = {

    changeSet(author: "tnguyen (generated)", id: "1570646389472-1") {
        createTable(tableName: "modelling_approach") {
            column(autoIncrement: "true", name: "id", type: "bigint") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "modelling_appPK")
            }

            column(name: "version", type: "bigint") {
                constraints(nullable: "false")
            }

            column(name: "accession", type: "varchar(64)") {
                constraints(nullable: "false")
            }

            column(name: "name", type: "varchar(255)") {
                constraints(nullable: "false")
            }

            column(name: "resource", type: "varchar(255)")
        }
    }

    changeSet(author: "tnguyen (generated)", id: "1570646389472-2") {
        addColumn(tableName: "model") {
            column(name: "modelling_approach_id", type: "bigint")
        }
    }

    changeSet(author: "tnguyen (generated)", id: "1570646389472-3") {
        addColumn(tableName: "revision") {
            column(name: "readme_submission", type: "varchar(2048)")
        }
    }

    changeSet(author: "tnguyen (generated)", id: "1570646389472-19") {
        createIndex(indexName: "FK_6lec3isrirxcu2nkigyr3sfcf", tableName: "model") {
            column(name: "modelling_approach_id")
        }
    }

    changeSet(author: "tnguyen (generated)", id: "1570646389472-5") {
        addForeignKeyConstraint(baseColumnNames: "modelling_approach_id", baseTableName: "model", constraintName: "FK_6lec3isrirxcu2nkigyr3sfcf", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "modelling_approach", referencesUniqueColumn: "false")
    }
}
