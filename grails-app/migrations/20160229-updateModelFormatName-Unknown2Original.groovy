/**
 * Created by Tung on 29/02/2016.
 */
databaseChangeLog = {
    changeSet(author: "Tung Nguyen", id: "20160229-131102-1") {
        grailsChange {
            change {
                String identifier='UNKNOWN'
                String name = 'Original code'
                def mFormat = sql.firstRow("select * from model_format where identifier = ${identifier}")
                if (mFormat) {
                    sql.executeUpdate("update model_format set name = ? where identifier = ?",
                        [name, identifier])
                }
            }
        }
    }
}
