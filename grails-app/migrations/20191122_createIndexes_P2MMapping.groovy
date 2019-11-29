databaseChangeLog = {

    changeSet(author: "tnguyen (manually created)", id: "1574430048000-1") {
        createIndex(indexName: "idx_p2m_rep", tableName: "p2mmapping") {
            column(name: "representative")
        }
    }
    changeSet(author: "tnguyen (manually created)", id: "1574430048000-2") {
        createIndex(indexName: "idx_p2m_mem", tableName: "p2mmapping") {
            column(name: "member")
        }
    }
}
