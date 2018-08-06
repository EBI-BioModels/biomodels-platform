package net.biomodels.jummp.qcinfo.authorisation

import grails.plugin.springsecurity.SpringSecurityUtils

/**
 * Created by tnguyen on 25/07/16.
 */
class CertificationAuthorisationService {
    def grailsApplication

    boolean isAllowed() {
        def allowedRoles = grailsApplication.config.jummp.security.certificationRole
        if (!allowedRoles) {
            log.error("""\
Role required for certification not defined.\
Please specify jummp.security.certificationRole in the configuration file.""")
        }
        SpringSecurityUtils.ifAnyGranted(allowedRoles)
    }
}
