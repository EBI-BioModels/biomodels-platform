/**
 * Created by Tung on 14/01/2016.
 */
databaseChangeLog = {
    changeSet(author: "Tung Nguyen", id: "20160114-162125") {
        modifyDataType(tableName: "resource_reference", columnName: "name", newDataType: "text")
    }
}
