import net.biomodels.jummp.plugins.security.Person
import net.biomodels.jummp.model.Publication
import net.biomodels.jummp.model.PublicationLinkProvider
import net.biomodels.jummp.model.PublicationPerson as PubPerson

databaseChangeLog = {

	changeSet(author: "raza (generated)", id: "1397230578681-1") {
		createTable(tableName: "publication_person") {
			column(name: "publication_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "person_id", type: "bigint") {
				constraints(nullable: "false")
			}

			column(name: "position", type: "integer") {
				constraints(nullable: "false")
			}

			column(name: "pub_alias", type: "varchar(255)") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "raza (generated)", id: "1397230578681-4") {
		addPrimaryKey(columnNames: "publication_id, person_id", constraintName: "publication_pPK", tableName: "publication_person")
	}

	changeSet(author: "raza (generated)", id: "1397230578681-7") {
		createIndex(indexName: "FKE7516CC8448831C4", tableName: "publication_person") {
			column(name: "person_id")
		}
	}

	changeSet(author: "raza (generated)", id: "1397230578681-8") {
		createIndex(indexName: "FKE7516CC87C6F423F", tableName: "publication_person") {
			column(name: "publication_id")
		}
	}

	changeSet(author: "raza (generated)", id: "1397230578681-5") {
		addForeignKeyConstraint(baseColumnNames: "person_id", baseTableName: "publication_person", constraintName: "FKE7516CC8448831C4", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "person", referencesUniqueColumn: "false")
	}

	changeSet(author: "raza (generated)", id: "1397230578681-6") {
		addForeignKeyConstraint(baseColumnNames: "publication_id", baseTableName: "publication_person", constraintName: "FKE7516CC87C6F423F", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "publication", referencesUniqueColumn: "false")
	}
	
	changeSet(author: "raza and mihai", id: "delete persons 1") {
		grailsChange {
			change{
				String query="delete from person where not exists (select * from user where person_id=person.id)";
				sql.execute(query);
			}
		}
	}
	changeSet(author: "raza and mihai", id: "import pubmed authors 2") {
		grailsChange {
			change{
				def pubs=Publication.list();
				def pubmedSource=[];
				pubs.each { publication ->
					if (publication.linkProvider.linkType == PublicationLinkProvider
																	.LinkType
																	.PUBMED) {
						pubmedSource.add(publication)
						url = new URL("http://www.ebi.ac.uk/europepmc/webservices/rest/search/query=ext_id:${publication.link}%20src:med&resulttype=core")
						try {
							def slurper = new XmlSlurper().parse(url.openStream())
							int counter=0;
							for (def authorXml in slurper.resultList.result.authorList.author) {
							 	 String alias = authorXml.fullName[0].text()
							 	 String orcid = null
							 	 Person person;
							 	 if (authorXml.authorId[0]?.@type=="ORCID") {
							 	 	 	orcid=authorXml.authorId[0].text()
							 	 	 	def candidate = Person.findByOrcid(orcid)
							 	 	 	if (candidate) {
							 	 	 		person=candidate;
							 	 		}	 	 
							 	 }
							 	 if (!person) {
							 	 	 person = new Person(userRealName: alias)
							 	 	 person.save();
							 	 }
							 	 PubPerson pubPerson=new PubPerson( person: person,
							 	 									pubAlias: alias, 
							 	 									position: counter++, 
							 	 									publication: publication );
							 	 pubPerson.save();
							}
						} 
						catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}	
	changeSet(author: "raza and mihai", id: "import non pubmed authors manually") {
		grailsChange {
			preConditions(onFail: 'MARK_RAN') {
				grailsPrecondition {
					check {
						def result = sql.firstRow "select COUNT(*) as pubs from publication"
						assert result.pubs > 0 : "publication table empty, no authors to process manually"
					}
				}
			}
			change {
				//def pubCount = sql.firstRow "select COUNT(*) as pubs from publication"
				//if (pubCount.pubs == 0) return;

				def customImport = { publicationID, alias, position ->
					Publication publication=Publication.get(publicationID)
					def person = new Person(userRealName: alias)
					person.save();
					PubPerson pubPerson=new PubPerson( person: person,
							 	 					   pubAlias: alias, 
							 	 		     		   position: position, 
							 	 					   publication: publication );
					pubPerson.save();
				}
			   customImport(5, "Peter L. Bonate", 0);
			   counter=0;
			   customImport(18, "Ribba B", counter++);
			   customImport(18, "Kaloshi G", counter++);
			   customImport(18, "Peyre M", counter++);
			   customImport(18, "Ricard D", counter++);
			   customImport(18, "Calvez V", counter++);
			   customImport(18, "Tod M", counter++);
			   customImport(18, "Cajavec-Bernard B", counter++);
			   customImport(18, "Idbaih A", counter++);
			   customImport(18, "Psimaras D", counter++);
			   customImport(18, "Dainese L", counter++);
			   customImport(18, "Pallud J", counter++);
			   customImport(18, "Cartalat-Carel S", counter++);
			   customImport(18, "Delattre JY", counter++);
			   customImport(18, "Honnorat J", counter++);
			   customImport(18, "Grenier E", counter++);
			   customImport(18, "Ducray F", counter++);
			   counter=0; //same paper imported with different dois?
			   customImport(21, "Ribba B", counter++);
			   customImport(21, "Kaloshi G", counter++);
			   customImport(21, "Peyre M", counter++);
			   customImport(21, "Ricard D", counter++);
			   customImport(21, "Calvez V", counter++);
			   customImport(21, "Tod M", counter++);
			   customImport(21, "Cajavec-Bernard B", counter++);
			   customImport(21, "Idbaih A", counter++);
			   customImport(21, "Psimaras D", counter++);
			   customImport(21, "Dainese L", counter++);
			   customImport(21, "Pallud J", counter++);
			   customImport(21, "Cartalat-Carel S", counter++);
			   customImport(21, "Delattre JY", counter++);
			   customImport(21, "Honnorat J", counter++);
			   customImport(21, "Grenier E", counter++);
			   customImport(21, "Ducray F", counter++);
			   counter=0;
			   customImport(22, "Jupp S", counter++);
			   customImport(22, "Malone J", counter++);
			   customImport(22, "Bolleman J", counter++);
			   customImport(22, "Brandizi M", counter++);
			   customImport(22, "Davies M", counter++);
			   customImport(22, "Garcia L", counter++);
			   customImport(22, "Gaulton A", counter++);
			   customImport(22, "Gehant S", counter++);
			   customImport(22, "Laibe C", counter++);
			   customImport(22, "Redaschi N", counter++);
			   customImport(22, "Wimalaratne SM", counter++);
			   customImport(22, "Martin M", counter++);
			   customImport(22, "Le Novère N", counter++);
			   customImport(22, "Parkinson H", counter++);
			   customImport(22, "Birney E", counter++);
			   customImport(22, "Jenkinson AM", counter++);
			   counter=0;
			   
			}
		}
	}	
	
}
