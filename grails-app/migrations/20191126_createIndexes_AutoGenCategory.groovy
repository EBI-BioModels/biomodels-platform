databaseChangeLog = {

    changeSet(author: "tnguyen (manually created)", id: "1574781131713-1") {
        createIndex(indexName: "dx_autogen_cate_model_id", tableName: "auto_generated_category") {
            column(name: "model_identifier")
        }
    }
    changeSet(author: "tnguyen (manually created)", id: "1574781131713-2") {
        createIndex(indexName: "idx_autogen_cate_name", tableName: "auto_generated_category") {
            column(name: "category_name")
        }
    }
}
