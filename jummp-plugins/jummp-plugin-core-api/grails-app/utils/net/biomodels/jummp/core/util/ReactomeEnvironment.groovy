package net.biomodels.jummp.core.util

import grails.util.Environment

/**
 * @author carankalle on 10/02/2020.
 */
enum ReactomeEnvironment {
    DEV_URL("https://dev.reactome.org/DiagramJs/diagram/diagram.nocache.js"),
    PROD_URL("https://reactome.org/DiagramJs/diagram/diagram.nocache.js")

    private String diagramJSUrl

    ReactomeEnvironment(String diagramJSUrl) {
        this.diagramJSUrl = diagramJSUrl
    }

    static String getUrlForThisEnvironment() {
        if(Environment.current == Environment.PRODUCTION) {
            PROD_URL.diagramJSUrl
        } else {
            DEV_URL.diagramJSUrl
        }
    }
}
