import net.biomodels.jummp.model.ModelFormat
import net.biomodels.jummp.model.ModelElementType

databaseChangeLog = {
    changeSet(author: "tnguyen (customised)", id: "1630057751-1") {
        grailsChange {
            change {
                def format = new ModelFormat(identifier: "MorpheusML", name: "MorpheusML", formatVersion: "*")
                // clear session and save last records
                ModelFormat.withSession { session ->
                    boolean respone = format.save()
                    if (respone) {
                        format = ModelFormat.findByIdentifier("MorpheusML")
                        if (format) {
                            def met = new ModelElementType(name: "model", modelFormat: format)
                            if (!met.save(flush: true)) {
                                println("Errors happened when trying to create a Model Element Type for the format " +
                                    "${format.dump()}")
                            }
                        }
                    }
                    session.flush()
                    session.clear()
                }
            }
        }
    }
}
