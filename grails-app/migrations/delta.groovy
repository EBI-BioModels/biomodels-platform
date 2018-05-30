databaseChangeLog = {
	changeSet(author: "mglont (generated)", id: "1487352784053-4") {
		createIndex(indexName: "FK_fhuoesmjef3mrv0gpja4shvcr", tableName: "acl_entry") {
			column(name: "acl_object_identity")
		}
	}

	changeSet(author: "mglont (generated)", id: "1487352784053-5") {
		createIndex(indexName: "FK_6c3ugmk053uy27bk2sred31lf", tableName: "acl_object_identity") {
			column(name: "object_id_class")
		}
	}

	changeSet(author: "mglont (generated)", id: "1487352784053-6") {
		createIndex(indexName: "label_uniq_1487352783587", tableName: "flag", unique: "true") {
			column(name: "label")
		}
	}

	changeSet(author: "mglont (generated)", id: "1487352784053-7") {
		createIndex(indexName: "FK_m8cyuas1h3xxjw3cum81xan4v", tableName: "model_flag") {
			column(name: "flag_id")
		}
	}

	changeSet(author: "mglont (generated)", id: "1487352784053-8") {
		createIndex(indexName: "FK_2s79xvpj2id83gdydk9x5pmg9", tableName: "model_history_item") {
			column(name: "model_id")
		}
	}

	changeSet(author: "mglont (generated)", id: "1487352784053-9") {
		createIndex(indexName: "FK_eqraoskk9sa8bw5ihjqj47h4a", tableName: "publication_person") {
			column(name: "publication_id")
		}
	}

	changeSet(author: "mglont (generated)", id: "1487352784053-10") {
		createIndex(indexName: "FK_etiwh5vidmknmf8dedkp4air0", tableName: "revision") {
			column(name: "model_id")
		}
	}

	changeSet(author: "mglont (generated)", id: "1487352784053-11") {
		createIndex(indexName: "FK_8brpctoynuteloeqtumijkyl4", tableName: "revision_annotations") {
			column(name: "revision_id")
		}
	}

	changeSet(author: "mglont (generated)", id: "1487352784053-12") {
		createIndex(indexName: "FK_frjxgaag9ypwhjyf7y3p5o3of", tableName: "team") {
			column(name: "owner_id")
		}
	}

	changeSet(author: "mglont (generated)", id: "1487352784053-13") {
		createIndex(indexName: "FK_it77eq964jhfqtu54081ebtio", tableName: "user_role") {
			column(name: "role_id")
		}
	}

	changeSet(author: "mglont (generated)", id: "1487352784053-14") {
		createIndex(indexName: "FK_keqg9kiqm4mga7l6u8c1a9898", tableName: "user_team") {
			column(name: "team_id")
		}
	}
}
