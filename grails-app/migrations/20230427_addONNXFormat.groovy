import net.biomodels.jummp.model.ModelFormat
import net.biomodels.jummp.model.ModelElementType

databaseChangeLog = {
    changeSet(author: "Tung Nguyen", id: "add ONNX format") {
        grailsChange {
            change {
                def format = new ModelFormat(identifier: "ONNX", name: "Open Neural Network Exchange", formatVersion: "*")
                // clear session and save last records
                ModelFormat.withSession { session ->
                    boolean response = format.save()
                    if (response) {
                        format = ModelFormat.findByIdentifier("ONNX")
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
