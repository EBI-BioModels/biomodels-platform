databaseChangeLog = {
    changeSet(author: "tnguyen (generated)", id: "1573216491257-1") {
        createIndex(indexName: "idx_ma_name", tableName: "modelling_approach") {
            column(name: "name")
        }
    }
}
