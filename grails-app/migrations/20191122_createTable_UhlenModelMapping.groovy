databaseChangeLog = {

    changeSet(author: "tnguyen (generated)", id: "1574426091965-1") {
        createTable(tableName: "uhlen_model_mapping") {
            column(name: "representative", type: "varchar(32)") {
                constraints(nullable: "false")
            }

            column(name: "member", type: "varchar(32)") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "tnguyen (generated)", id: "1574426091965-2") {
        addPrimaryKey(columnNames: "representative, member", constraintName: "uhlen_model_mPK",
            tableName: "uhlen_model_mapping")
    }

    changeSet(author: "tnguyen (manually created)", id: "1574426091965-3") {
        createIndex(indexName: "idx_uhlen_rep", tableName: "uhlen_model_mapping") {
            column(name: "representative")
        }
    }
    changeSet(author: "tnguyen (manually created)", id: "1574426091965-4") {
        createIndex(indexName: "idx_uhlen_mem", tableName: "uhlen_model_mapping") {
            column(name: "member")
        }
    }
}
