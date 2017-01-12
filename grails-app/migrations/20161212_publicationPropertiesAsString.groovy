databaseChangeLog = {
    /**
     * as the conversion is from integer to varchar, we don't need to manually cast existing column values.
     */
    changeSet(author: "mglont", id: "1481556058271-1") {
        modifyDataType(columnName: "issue", newDataType: "varchar(255)", tableName: "publication")
    }

    changeSet(author: "mglont", id: "1481556058271-2") {
        modifyDataType(columnName: "volume", newDataType: "varchar(255)", tableName: "publication")
    }

    changeSet(author: "mglont", id: "1481556058271-3") {
        modifyDataType(columnName: "affiliation", newDataType: "mediumtext", tableName: "publication")
    }
}
