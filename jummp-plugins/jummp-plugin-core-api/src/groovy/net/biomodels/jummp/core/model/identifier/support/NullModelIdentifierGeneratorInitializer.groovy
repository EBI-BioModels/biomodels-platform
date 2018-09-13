package net.biomodels.jummp.core.model.identifier.support

class NullModelIdentifierGeneratorInitializer implements ModelIdentifierGeneratorInitializer {
    @Override
    String getLastUsedValue() {
        return null
    }
}
