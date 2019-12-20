import net.biomodels.jummp.annotationstore.ResourceReference
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.model.ModellingApproach

databaseChangeLog = {
    changeSet(author: "tnguyen (generated)", id: "1574437108000-1") {
        grailsChange {
            change {
                Map<String, String> MODELLING_APPROACHES = new HashMap<>()
                ModellingApproach.list().each { ModellingApproach approach ->
                    MODELLING_APPROACHES.put(approach.accession, approach.name)
                }
                def resultSet = Model.executeQuery """
                    select m, r, xref
                    from RevisionAnnotation ra
                        join ra.revision as r
                        join ra.elementAnnotation as ea
                        join ea.statement as s
                        join s.object as xref
                        join r.model as m
                    where
                        m.deleted = false
                        and xref.datatype = 'mamo'
                        and r.revisionNumber = (
                            select max(revisionNumber) from Revision r2
                            where r2.model = m
                        )
                    group by m
                """, [readOnly: true]
                resultSet.eachWithIndex { Model model, Revision revision, ResourceReference mamoXref, int index ->
                     String mamoTermLabel = mamoXref.accession
                    if (MODELLING_APPROACHES.containsKey(mamoTermLabel)) {
                        // update modelling approach
                        ModellingApproach approach = ModellingApproach.findByAccession(mamoTermLabel)
                        model.modellingApproach = approach
                        if (model.save(flush: true)) {
                            println """\
set modelling approach ${approach.properties} to model ${model.properties} successfully"""
                        } else {
                            String msg = """\
cannot update modelling apporach ${approach.properties} to model ${model.properties}"""
                            throw new IllegalStateException(msg)
                        }
                    } else {
                        println "cannot find any MAMO term describing a modelling approach"
                    }

                    // clear session and save records after every 100 entries created
                    if (index % 100 == 0) {
                        Model.withSession { session ->
                            session.flush()
                            session.clear()
                        }
                    }
                }

                // clear session and save last records
                Model.withSession { session ->
                    session.flush()
                    session.clear()
                }
            }
        }
    }
}
