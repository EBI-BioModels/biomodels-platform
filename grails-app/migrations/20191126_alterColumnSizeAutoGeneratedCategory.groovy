databaseChangeLog = {

    changeSet(author: "tnguyen (manually created)", id: "1574781131713-3") {
        modifyDataType(tableName: "auto_generated_category",
            columnName: "model_identifier", newDataType: "varchar(32)")
    }
}
