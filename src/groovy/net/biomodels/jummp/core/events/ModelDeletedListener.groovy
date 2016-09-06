package net.biomodels.jummp.core.events

import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationListener

import org.apache.log4j.Logger

/**
 * Created by tnguyen on 05/09/16.
 */
class ModelDeletedListener implements ApplicationListener {
     /**
     * The logger for this class
     */
    Logger log = Logger.getLogger(getClass())

    def modelDelegateService

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ModelDeletedEvent) {
            log.info("$event deleted $event.source")
        }
    }
}
