databaseChangeLog = {

	changeSet(author: "mglont (generated)", id: "1486387545035-1") {
		createTable(tableName: "revision_annotations") {
			column(name: "revision_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "element_annotation_id", type: "bigint") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "mglont (generated)", id: "1486387545035-2") {
		addPrimaryKey(columnNames: "revision_id, element_annotation_id", tableName: "revision_annotations")
	}

	changeSet(author: "mglont (generated)", id: "1486387545035-3") {
		dropForeignKeyConstraint(baseTableName: "element_annotation", constraintName: "FK_nf3bp0l5t5ap8oi168mtpgued")
        preConditions(onFail: "MARK_RAN") {
           foreignKeyConstraintExists(foreignKeyName: "miriam_identifier")
        }
	}

	changeSet(author: "mglont (generated)", id: "1486387545035-4") {
		createIndex(indexName: "FK_4kjivx7p0k1lymrcsbxn0w83o", tableName: "revision_annotations") {
			column(name: "element_annotation_id")
		}
	}

	changeSet(author: "mglont (generated)", id: "1486387545035-5") {
		createIndex(indexName: "FK_8brpctoynuteloeqtumijkyl4", tableName: "revision_annotations") {
			column(name: "revision_id")
		}
	}

    changeSet(author: "mglont", id: "1486387545035-6") {
        grailsChange {
            change {
                def anno = sql.dataSet("element_annotation")
                def rev_anno = sql.dataSet("revision_annotations")
                anno.rows().each { a ->
                    def id = a.id
                    def rId = a.revision_id
                    try {
                        rev_anno.add(revision_id: rId, element_annotation_id: id)
                    } catch(java.sql.SQLException e) {
                        println "Migration of annotation $a failed: $e"
                    }
                }
            }
        }
    }

	changeSet(author: "mglont (generated)", id: "1486387545035-7") {
		dropForeignKeyConstraint(baseTableName: "element_annotation", constraintName: "FKCFAB2C52265115F5")
		dropColumn(columnName: "revision_id", tableName: "element_annotation")
        preConditions(onFail: "MARK_RAN") {
            columnExists(columnName: "revision_id", tableName: "element_annotation")
        }
	}

	changeSet(author: "mglont (generated)", id: "1486387545035-8") {
		addForeignKeyConstraint(baseColumnNames: "element_annotation_id", baseTableName: "revision_annotations", constraintName: "FK_4kjivx7p0k1lymrcsbxn0w83o", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "element_annotation", referencesUniqueColumn: "false")
	}

	changeSet(author: "mglont (generated)", id: "1486387545035-9") {
		addForeignKeyConstraint(baseColumnNames: "revision_id", baseTableName: "revision_annotations", constraintName: "FK_8brpctoynuteloeqtumijkyl4", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "revision", referencesUniqueColumn: "false")
	}
}
