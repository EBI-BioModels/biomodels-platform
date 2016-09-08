package net.biomodels.jummp.core.events

import net.biomodels.jummp.model.Model
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationListener

import org.apache.log4j.Logger

/**
 * Created by tnguyen on 05/09/16.
 */
class ModelDeletedListener implements ApplicationListener<ModelDeletedEvent> {
     /**
     * The logger for this class
     */
    Logger log = Logger.getLogger(getClass())

    def modelDelegateService

    public void onApplicationEvent(ModelDeletedEvent event) {
        if (event instanceof ModelDeletedEvent) {
            log.info("$event called for deleting the resource: $event.source")
            println("$event called for deleting the resource: $event.source")
            //Model.deleteAll(event.model.id)
        }
    }
}
