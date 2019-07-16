databaseChangeLog = {

    changeSet(author: "tnguyen (generated)", id: "1560526538474-1") {
        createTable(tableName: "p2mmapping") {
            column(name: "representative", type: "varchar(32)") {
                constraints(nullable: "false")
            }

            column(name: "member", type: "varchar(32)") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "tnguyen (generated)", id: "1560526538474-2") {
        addPrimaryKey(columnNames: "representative, member", constraintName: "PK_p2mmapping", tableName: "p2mmapping")
    }

    changeSet(author: "tnguyen (generated)", id: "1560526538474-3") {
        createIndex(indexName: "IDX_representative_p2m", tableName: "p2mmapping") {
            column(name: "representative")
        }
    }

    changeSet(author: "tnguyen (generated)", id: "1560526538474-4") {
        createIndex(indexName: "IDX_member_p2m", tableName: "p2mmapping") {
            column(name: "member")
        }
    }
}
