package net.biomodels.jummp.search

import net.biomodels.jummp.core.ModelSearchStrategy
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand

/**
 * Created by Tung on 30/08/2016.
 */
class OmicsDIBasedSearch implements ModelSearchStrategy {
    def omicsDiHolder

    String name() {
        return "omicsdi"
    }

    void clearIndex() {
        omicsDiHolder.doSomeStuff()
    }

    void clearAnnotationStatementsFromDatabase() {

    }

    boolean isCertified(def rev) {
        return false
    }

    boolean isDeleted(def model) {
        return false
    }

    void makePublic(def revision) {

    }

    void regenerateIndices() {
    }

    void setCertified(def rev, boolean value = true) {
    }

    void setDeleted(def model, boolean deleted = true) {
    }

    Collection<ModelTransportCommand> searchModels(String query) {
        return new ArrayList<ModelTransportCommand>()
    }

    void updateIndex(RevisionTransportCommand revision) {
    }
}
