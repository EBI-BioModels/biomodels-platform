package net.biomodels.jummp.core.events

import net.biomodels.jummp.core.model.ModelTransportCommand

/**
 * Created by tnguyen on 31/08/16.
 */
class ModelDeletedEvent extends JummpEvent {
    /**
     * The newly deleted model.
     */
    final ModelTransportCommand model

    ModelDeletedEvent() {
        super([])
    }

    ModelDeletedEvent(Object source, final ModelTransportCommand model) {
        super(source)
        this.model = model
    }
}
