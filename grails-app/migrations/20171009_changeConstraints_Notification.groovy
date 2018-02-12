/**
 * Created by tnguyen@ebi.ac.uk on 09/10/17.
 */
databaseChangeLog = {
    changeSet(author: "Tung Nguyen", id: "20171009120820-1") {
        modifyDataType(tableName: "notification", columnName: "body",
            newDataType: "varchar(1024)")
    }
}
