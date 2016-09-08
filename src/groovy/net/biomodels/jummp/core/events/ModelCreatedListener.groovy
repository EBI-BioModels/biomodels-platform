package net.biomodels.jummp.core.events

import org.apache.log4j.Logger
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationListener

/**
 * Created by tnguyen on 05/09/16.
 */
class ModelCreatedListener implements ApplicationListener {
    /**
     * The logger for this class
     */
    Logger log = Logger.getLogger(getClass())

    void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ModelCreatedEvent) {
            log.info("Tung Nguyen: $event created the model $event.source")
        }
    }
}
