databaseChangeLog = {
    changeSet(author: "mglont", id: "revisionNumberIdx") {
        createIndex(indexName: "revision_number", tableName: "revision") {
            column(name: "revision_number")
        }
    }

    changeSet(author: "mglont", id: "revisionDateIdx") {
        createIndex(indexName: "upload_date", tableName: "revision") {
            column(name: "upload_date")
        }
    }
}
