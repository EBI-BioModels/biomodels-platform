import net.biomodels.jummp.model.ModelFormat
import net.biomodels.jummp.model.ModelElementType

databaseChangeLog = {
    changeSet(author: "Tung Nguyen", id: "add CC3DML format") {
        grailsChange {
            change {
                def format = new ModelFormat(identifier: "CC3DML", name: "CompuCell3DML", formatVersion: "*")
                // clear session and save last records
                ModelFormat.withSession { session ->
                    boolean response = format.save()
                    if (response) {
                        format = ModelFormat.findByIdentifier("CC3DML")
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
