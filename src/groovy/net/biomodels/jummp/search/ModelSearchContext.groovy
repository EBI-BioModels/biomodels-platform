package net.biomodels.jummp.search

import net.biomodels.jummp.core.ModelSearchStrategy
import net.biomodels.jummp.core.model.RevisionTransportCommand

/**
 * Created by Tung on 30/08/2016.
 */
class ModelSearchContext {
    private ModelSearchStrategy strategy

    ModelSearchContext(ModelSearchStrategy strategy) {
        this.strategy = strategy
    }

    // this can be set at runtime by the application references
    void setupSearchStrategy(String cfgSearchStrategy) {
        this.strategy = cfgSearchStrategy.equalsIgnoreCase("omicsdi") ? new OmicsDIBasedSearch() : new SolrBasedSearch()
    }

    String getStrategyName() {
        return strategy.name()
    }
    // use the strategy

    void searchModels() {

    }

    List<String> fetchFilesFromRevision(RevisionTransportCommand rev, boolean filterMains) {
        if (filterMains) {
            return rev?.files?.findAll{it.mainFile}.collect{it.path}
        }
        return rev?.files?.collect{it.path}
    }
}
