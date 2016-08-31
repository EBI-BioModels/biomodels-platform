package net.biomodels.jummp.search

import net.biomodels.jummp.core.ModelSearchStrategy

/**
 * Created by Tung on 30/08/2016.
 */
class ModelSearchContext {
    private ModelSearchStrategy strategy

    ModelSearchContext(ModelSearchStrategy strategy) {
        this.strategy = strategy
    }

    // this can be set at runtime by the application references
    void setup(String cfgSearchStrategy) {
        this.strategy = (cfgSearchStrategy == "omicsdi") ? new OmicsDiHandler() : new SolrServerHolder()
    }

    // use the strategy

    void searchModels() {

    }
}
