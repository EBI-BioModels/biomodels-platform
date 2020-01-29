/**
 * Created by Tung on 23/01/2020.
 */
databaseChangeLog = {
    changeSet(author: "Tung Nguyen (manually created)", id: "1579805735-1") {
        grailsChange {
            change {
                String identifier='UNKNOWN'
                String name = 'Other'
                def mFormat = sql.firstRow("select * from model_format where identifier = ${identifier}")
                if (mFormat) {
                    sql.executeUpdate("update model_format set name = ? where identifier = ?",
                        [name, identifier])
                }
            }
        }
    }
}
