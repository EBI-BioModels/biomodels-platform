package net.biomodels.jummp.core.model.identifier

import groovy.transform.CompileStatic
import net.biomodels.jummp.core.model.identifier.support.ModelIdentifierGeneratorInitializer

@CompileStatic
class DummyModelIdentifierInitializer implements ModelIdentifierGeneratorInitializer {
    String lastUsedValue

    DummyModelIdentifierInitializer(String lastUsedValue) {
        this.lastUsedValue = lastUsedValue
    }
}
