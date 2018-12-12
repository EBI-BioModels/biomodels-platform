package net.biomodels.jummp.deployment.biomodels

import grails.plugin.springsecurity.annotation.Secured

/**
 * This controller aims to serve special features
 */
class FeatureController {
    @Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
    def agedbrain() {
    }
}
