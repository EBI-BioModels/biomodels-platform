databaseChangeLog = {
    changeSet(author: "tnguyen (generated)", id: "1547485066-1") {
        createIndex(indexName: "unique_link", tableName: "publication", unique: "true") {
            column(name: "link_provider_id")
            column(name: "link")
        }
    }
}
