databaseChangeLog = {
    changeSet(author: "mglont (generated)", id: "1428594869002-1") {
        dropForeignKeyConstraint(baseTableName: "gene_ontology", constraintName: "FKF242873932FC8D7C")
    }

    changeSet(author: "mglont (generated)", id: "1428594869002-2") {
        dropForeignKeyConstraint(baseTableName: "gene_ontology_relationship", constraintName: "FKFBB69D7E2DF0334A")
    }

    changeSet(author: "mglont (generated)", id: "1428594869002-3") {
        dropForeignKeyConstraint(baseTableName: "gene_ontology_relationship", constraintName: "FKFBB69D7E57F37ED9")
    }

    changeSet(author: "mglont (generated)", id: "1428594869002-4") {
        dropForeignKeyConstraint(baseTableName: "gene_ontology_revision", constraintName: "FK8CD9722112342382")
    }

    changeSet(author: "mglont (generated)", id: "1428594869002-5") {
        dropForeignKeyConstraint(baseTableName: "gene_ontology_revision", constraintName: "FK8CD97221265115F5")
    }

    changeSet(author: "mglont (generated)", id: "1428594869002-6") {
        dropForeignKeyConstraint(baseTableName: "miriam_datatype", constraintName: "FK229D59E41FE7B37C")
        preConditions(onFail: "MARK_RAN") {
            tableExists(tableName: "miriam_datatype")
        }
    }

    changeSet(author: "mglont (generated)", id: "1428594869002-7") {
        dropForeignKeyConstraint(baseTableName: "miriam_identifier", constraintName: "FKB846CA93E20DD0F")
        preConditions(onFail: "MARK_RAN") {
            tableExists(tableName: "miriam_identifier")
        }
    }

    changeSet(author: "mglont (generated)", id: "1428594869002-8") {
        dropForeignKeyConstraint(baseTableName: "miriam_resource", constraintName: "FKA397840E3E20DD0F")
        preConditions(onFail: "MARK_RAN") {
            tableExists(tableName: "miriam_resource")
        }
    }

    changeSet(author: "mglont (generated)", id: "1428594869002-12") {
        dropIndex(indexName: "identifier", tableName: "miriam_datatype")
        preConditions(onFail: "MARK_RAN") {
            tableExists(tableName: "miriam_datatype")
        }
    }

    changeSet(author: "mglont (generated)", id: "1428594869002-13") {
        dropIndex(indexName: "identifier", tableName: "miriam_identifier")
        preConditions(onFail: "MARK_RAN") {
            tableExists(tableName: "miriam_identifier")
			indexExists(schemaName: "miriam_datatype", indexName: "identifier")
        }
    }

    changeSet(author: "mglont (generated)", id: "1428594869002-14") {
        dropIndex(indexName: "identifier", tableName: "miriam_resource")
        preConditions(onFail: "MARK_RAN") {
            tableExists(tableName: "miriam_resource")
			indexExists(schemaName: "miriam_resource", indexName: "identifier")
        }

    }

    changeSet(author: "mglont (generated)", id: "1428594869002-15") {
        dropTable(tableName: "gene_ontology")
    }

    changeSet(author: "mglont (generated)", id: "1428594869002-16") {
        dropTable(tableName: "gene_ontology_relationship")
    }

    changeSet(author: "mglont (generated)", id: "1428594869002-17") {
        dropTable(tableName: "gene_ontology_revision")
    }

    changeSet(author: "mglont (generated)", id: "1428594869002-18") {
        dropTable(tableName: "miriam_datatype")
        preConditions(onFail: "MARK_RAN") {
            tableExists(tableName: "miriam_datatype")
        }
    }

    changeSet(author: "mglont (generated)", id: "1428594869002-19") {
        dropTable(tableName: "miriam_identifier")
        preConditions(onFail: "MARK_RAN") {
            tableExists(tableName: "miriam_identifier")
        }
    }

    changeSet(author: "mglont (generated)", id: "1428594869002-20") {
        dropTable(tableName: "miriam_resource")
        preConditions(onFail: "MARK_RAN") {
            tableExists(tableName: "miriam_resource")
        }
    }
}

