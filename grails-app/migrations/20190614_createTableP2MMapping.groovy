databaseChangeLog = {

    changeSet(author: "tnguyen (generated)", id: "1560526538474-1") {
        createTable(tableName: "p2mmapping") {
            column(name: "representative_id", type: "bigint") {
                constraints(nullable: "false")
            }

            column(name: "member", type: "varchar(255)") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "tnguyen (generated)", id: "1560526538474-3") {
        addPrimaryKey(columnNames: "representative_id, member", constraintName: "p2mmappingPK", tableName: "p2mmapping")
    }

    changeSet(author: "tnguyen (generated)", id: "1560526538474-12") {
        createIndex(indexName: "FK_dh8de8m6wv6566j4bg40hcdcn", tableName: "p2mmapping") {
            column(name: "representative_id")
        }
    }

    changeSet(author: "tnguyen (generated)", id: "1560526538474-4") {
        addForeignKeyConstraint(baseColumnNames: "representative_id", baseTableName: "p2mmapping",
            constraintName: "FK_dh8de8m6wv6566j4bg40hcdcn", deferrable: "false",
            initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "model",
            referencesUniqueColumn: "false")
    }
}
