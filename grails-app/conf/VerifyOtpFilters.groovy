/**
* Copyright (C) 2010-2016 EMBL-European Bioinformatics Institute (EMBL-EBI),
* Deutsches Krebsforschungszentrum (DKFZ)
*
* This file is part of Jummp.
*
* Jummp is free software; you can redistribute it and/or modify it under the
* terms of the GNU Affero General Public License as published by the Free
* Software Foundation; either version 3 of the License, or (at your option) any
* later version.
*
* Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
* A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
* details.
*
* You should have received a copy of the GNU Affero General Public License along
* with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
**/

import grails.plugin.springsecurity.annotation.Secured as GrailsSecured
import org.springframework.security.access.annotation.Secured as SpringSecured

class VerifyOtpFilters {
    def grailsApplication
    def authService
    def springSecurityService

    List IGNORED_ACTIONS = [
        "load2fa", "enrollTwoFactor", "verifyOTP", "generateOTP", "checkTrustDevice", "updateTrustDeviceOnRedis", "toggle2FA"
    ]

    private static final List PUBLIC_RULES = [
        'permitAll', 'IS_AUTHENTICATED_ANONYMOUSLY'
    ]

    private boolean isPublicAction(String controllerName, String actionName) {
        def artifact = grailsApplication.getArtefactByLogicalPropertyName("Controller", controllerName)
        if (!artifact) return true // unknown controller; Spring Security's own rules apply

        Class clazz = artifact.clazz
        def method = clazz.methods.find { it.name == actionName }
        def annotation = method?.getAnnotation(GrailsSecured) ?:
                         method?.getAnnotation(SpringSecured) ?:
                         clazz.getAnnotation(GrailsSecured) ?:
                         clazz.getAnnotation(SpringSecured)

        if (!annotation) return true // no @Secured annotation; defer to Spring Security
        return annotation.value().any { it in PUBLIC_RULES }
    }

    def filters = {
        verifyOTP(controller:'*', action:'*') {
            before = {
                // Config is read on every request (cheap). The DB call (is2FAEnabled) is made
                // only once per session and cached; flipping the config flag off clears the
                // banner immediately without waiting for the user to log out.
                try {
                    if (springSecurityService.isLoggedIn()) {
                        boolean enrollmentNotice = grailsApplication.config.jummp.security.twofa.enrollmentNotice ?: false
                        boolean enforced = grailsApplication.config.jummp.security.twofa.enforced ?: false
                        if (!enrollmentNotice || enforced) {
                            session.showEnrollmentNotice = false
                        } else if (session.getAttribute("showEnrollmentNotice") == null) {
                            String username = springSecurityService.currentUser?.username
                            session.showEnrollmentNotice = username ? !authService.is2FAEnabled(username) : false
                        }
                    }
                } catch (Exception e) {
                    log.warn("Could not evaluate enrollment notice state: ${e.message}")
                }

                String controller = params.get("controller")
                String action = params.get("action")
                if (controller && action) {
                    if (session.enabled2FA
                            && !action.contains("error")
                            && !IGNORED_ACTIONS.contains(action)
                            && !["notification"].contains(controller)) {
                        if (!isPublicAction(controller, action)) {
                            String targetAction = session.pendingEnrollment ? "enrollTwoFactor" : "load2fa"
                            redirect(controller: "auth", action: targetAction)
                            return false
                        }
                    }
                }
            }
            after = { Map model -> }
            afterView = { Exception e -> }
        }
    }
}
