databaseChangeLog = {

	changeSet(author: "tnguyen (generated)", id: "1703244723008-createPostTable") {
		createTable(tableName: "post") {
			column(autoIncrement: "true", name: "id", type: "bigint") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "postPK")
			}

			column(name: "version", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "content", type: "varchar(256)") {
				constraints(nullable: "false")
			}

			column(name: "post_by_id", type: "bigint") {
				constraints(nullable: "false")
			}
		}
	}

	/*changeSet(author: "tnguyen (generated)", id: "1703244723008-2") {
		addNotNullConstraint(columnDataType: "varchar(1024)", columnName: "body", tableName: "notification")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-3") {
		dropForeignKeyConstraint(baseTableName: "tag_links", baseTableSchemaName: "jummp-biomodels", constraintName: "FK_lmil1jg72pjc8ei5p6kk5g9un")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-11") {
		dropIndex(indexName: "idx_autogen_cate_model_id", tableName: "auto_generated_category")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-12") {
		dropIndex(indexName: "idx_autogen_cate_name", tableName: "auto_generated_category")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-13") {
		dropIndex(indexName: "idx_ma_name", tableName: "modelling_approach")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-14") {
		dropIndex(indexName: "IDX_member_p2m", tableName: "p2mmapping")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-15") {
		dropIndex(indexName: "IDX_representative_p2m", tableName: "p2mmapping")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-16") {
		dropIndex(indexName: "idx_p2m_mem", tableName: "p2mmapping")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-17") {
		dropIndex(indexName: "idx_p2m_rep", tableName: "p2mmapping")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-18") {
		dropIndex(indexName: "unique_link", tableName: "publication")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-19") {
		dropIndex(indexName: "Accession_Index", tableName: "resource_reference")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-20") {
		dropIndex(indexName: "revision_number", tableName: "revision")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-21") {
		dropIndex(indexName: "upload_date", tableName: "revision")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-22") {
		dropIndex(indexName: "UK_t48xdq560gs3gap9g7jg36kgc", tableName: "tags")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-23") {
		dropIndex(indexName: "idx_uhlen_mem", tableName: "uhlen_model_mapping")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-24") {
		dropIndex(indexName: "idx_uhlen_rep", tableName: "uhlen_model_mapping")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-25") {
		dropIndex(indexName: "FK_1i8ibd0dm67mut0hh09oqt452", tableName: "wcm_cnt2")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-26") {
		dropIndex(indexName: "FK_8duair0heuk4lw83fsp2arsce", tableName: "wcm_cnt2")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-27") {
		dropIndex(indexName: "FK_c9u6eo21g7eku09byvrsjjm0s", tableName: "wcm_cnt2")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-28") {
		dropIndex(indexName: "FK_qs79ql67napbqpkvni2tw6ha0", tableName: "wcm_cnt2")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-29") {
		dropIndex(indexName: "FK_sgkk9pe0yu0f2brjsl8j20svy", tableName: "wcm_cnt2")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-30") {
		dropIndex(indexName: "content_aliasURI_Idx", tableName: "wcm_cnt2")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-31") {
		dropIndex(indexName: "content_changedOn_Idx", tableName: "wcm_cnt2")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-32") {
		dropIndex(indexName: "content_contentName_Idx", tableName: "wcm_cnt2")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-33") {
		dropIndex(indexName: "content_createdOn_Idx", tableName: "wcm_cnt2")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-34") {
		dropIndex(indexName: "content_space_Idx", tableName: "wcm_cnt2")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-35") {
		dropIndex(indexName: "unique_aliasuri", tableName: "wcm_cnt2")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-36") {
		dropIndex(indexName: "FK_1i8ibd0dm67mut0hh09oqt452", tableName: "wcm_content")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-37") {
		dropIndex(indexName: "FK_8duair0heuk4lw83fsp2arsce", tableName: "wcm_content")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-38") {
		dropIndex(indexName: "FK_c9u6eo21g7eku09byvrsjjm0s", tableName: "wcm_content")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-39") {
		dropIndex(indexName: "FK_qs79ql67napbqpkvni2tw6ha0", tableName: "wcm_content")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-40") {
		dropIndex(indexName: "FK_sgkk9pe0yu0f2brjsl8j20svy", tableName: "wcm_content")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-41") {
		dropIndex(indexName: "content_aliasURI_Idx", tableName: "wcm_content")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-42") {
		dropIndex(indexName: "content_changedOn_Idx", tableName: "wcm_content")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-43") {
		dropIndex(indexName: "content_contentName_Idx", tableName: "wcm_content")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-44") {
		dropIndex(indexName: "content_createdOn_Idx", tableName: "wcm_content")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-45") {
		dropIndex(indexName: "content_space_Idx", tableName: "wcm_content")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-46") {
		dropIndex(indexName: "unique_aliasuri", tableName: "wcm_content")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-47") {
		dropIndex(indexName: "FK_gunqv3de4a2aowjtox5l10ug4", tableName: "wcm_related_content")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-48") {
		dropIndex(indexName: "FK_oxph3lt0u4eteq81gnh2dljrd", tableName: "wcm_related_content")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-49") {
		dropIndex(indexName: "UK_gkc6ixn9qkooura2r6lga6wll", tableName: "wcm_space")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-50") {
		dropIndex(indexName: "UK_hxt5tmvc0vgitd6qmw3yfd6or", tableName: "wcm_space")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-51") {
		dropIndex(indexName: "space_aliasURI_Idx", tableName: "wcm_space")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-52") {
		dropIndex(indexName: "space_name_Idx", tableName: "wcm_space")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-53") {
		createIndex(indexName: "FK_218s9w731wllynchepjvhb0eh", tableName: "contribution_details") {
			column(name: "role_id")
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-54") {
		createIndex(indexName: "FK_7aw1u6d16fk6dpu21x2sv9bmu", tableName: "contribution_details") {
			column(name: "revision_id")
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-55") {
		createIndex(indexName: "FK_dm2tsvy61mgariputdcy0gsmg", tableName: "contribution_details") {
			column(name: "contributor_id")
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-56") {
		createIndex(indexName: "FK_192yj1slixr6439vtvoaejldw", tableName: "contribution_invite") {
			column(name: "role_id")
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-57") {
		createIndex(indexName: "FK_3ol60twku48u5jfo20mss3u8x", tableName: "contribution_invite") {
			column(name: "revision_id")
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-58") {
		createIndex(indexName: "FK_gc9wng3us8nf331g2xiy837t1", tableName: "contribution_invite") {
			column(name: "inviter_id")
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-59") {
		createIndex(indexName: "reference_uniq_1703244722517", tableName: "contribution_invite", unique: "true") {
			column(name: "reference")
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-60") {
		createIndex(indexName: "FK_enuhed8tj1c7wlspwowtue3r6", tableName: "post") {
			column(name: "post_by_id")
		}
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-61") {
		dropTable(tableName: "tag_links")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-62") {
		dropTable(tableName: "tags")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-63") {
		dropTable(tableName: "wcm_cnt2")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-64") {
		dropTable(tableName: "wcm_content")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-65") {
		dropTable(tableName: "wcm_content_version")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-66") {
		dropTable(tableName: "wcm_related_content")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-67") {
		dropTable(tableName: "wcm_space")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-68") {
		dropTable(tableName: "wcm_status")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-4") {
		addForeignKeyConstraint(baseColumnNames: "contributor_id", baseTableName: "contribution_details", constraintName: "FK_dm2tsvy61mgariputdcy0gsmg", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "user", referencesUniqueColumn: "false")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-5") {
		addForeignKeyConstraint(baseColumnNames: "revision_id", baseTableName: "contribution_details", constraintName: "FK_7aw1u6d16fk6dpu21x2sv9bmu", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "revision", referencesUniqueColumn: "false")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-6") {
		addForeignKeyConstraint(baseColumnNames: "role_id", baseTableName: "contribution_details", constraintName: "FK_218s9w731wllynchepjvhb0eh", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "contribution_role", referencesUniqueColumn: "false")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-7") {
		addForeignKeyConstraint(baseColumnNames: "inviter_id", baseTableName: "contribution_invite", constraintName: "FK_gc9wng3us8nf331g2xiy837t1", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "user", referencesUniqueColumn: "false")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-8") {
		addForeignKeyConstraint(baseColumnNames: "revision_id", baseTableName: "contribution_invite", constraintName: "FK_3ol60twku48u5jfo20mss3u8x", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "revision", referencesUniqueColumn: "false")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-9") {
		addForeignKeyConstraint(baseColumnNames: "role_id", baseTableName: "contribution_invite", constraintName: "FK_192yj1slixr6439vtvoaejldw", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "contribution_role", referencesUniqueColumn: "false")
	}

	changeSet(author: "tnguyen (generated)", id: "1703244723008-10") {
		addForeignKeyConstraint(baseColumnNames: "post_by_id", baseTableName: "post", constraintName: "FK_enuhed8tj1c7wlspwowtue3r6", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "user", referencesUniqueColumn: "false")
	}*/
}
