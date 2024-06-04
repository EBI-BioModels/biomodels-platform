databaseChangeLog = {
    changeSet(author: "Tung Nguyen", id: "1717544352-1") {
        modifyDataType(tableName: "person", columnName: "institution", newDataType: "varchar(1024)")
    }
}
