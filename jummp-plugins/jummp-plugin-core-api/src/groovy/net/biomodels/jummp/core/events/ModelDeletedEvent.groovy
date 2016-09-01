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
    /**
     * The actual files which were uploaded.
     */
    final List<File> files

    ModelDeletedEvent(Object source, final ModelTransportCommand model, final List<File> files) {
        super(source)
        this.model = model
        this.files = files
    }
}
