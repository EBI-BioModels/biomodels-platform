package net.biomodels.jummp.core.model.identifier.support

import groovy.transform.CompileStatic
import net.biomodels.jummp.core.model.identifier.decorator.OrderedModelIdentifierDecorator

@CompileStatic
class GeneratorDetails {
    SortedSet<? extends OrderedModelIdentifierDecorator> decorators
    String regex
}
