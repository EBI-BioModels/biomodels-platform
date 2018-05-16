databaseChangeLog = {
    changeSet(author: "mglont (generated)", id: "1526398531230-7") {
        createIndex(indexName: "model_deleted", tableName: "model") {
            column(name: "deleted")
        }
    }

    changeSet(author: "mglont (generated)", id: "1526398531230-8") {
        createIndex(indexName: "person_name", tableName: "person") {
            column(name: "user_real_name")
        }
    }
}
