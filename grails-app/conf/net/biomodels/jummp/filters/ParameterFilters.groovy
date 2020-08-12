/**
* Copyright (C) 2010-2020 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
*/





package net.biomodels.jummp.filters

import net.biomodels.jummp.utils.InputParameterSanitizer
import org.codehaus.groovy.grails.commons.GrailsClass

/**
 * Filters for user input and parameters
 *
 * All parameters fetched from user interfaces will be securely encoded as HTML before feeding any business services.
 * The built-in parameters should be ignored, for example, controller and action name.
 *
 * @author <a href="mailto:tung.nguyen@ebi.ac.uk">Tung Nguyen</a>
 */
class ParameterFilters {
    def grailsApplication

    def filters = {
        all(controller:'*', action:'*') {
            before = {
                String controllerName = params["controller"]
                GrailsClass clazz = grailsApplication.getArtefactByLogicalPropertyName("Controller", controllerName)
                String fullClassName = clazz?.getFullName()
                if (!fullClassName) {
                    List allClasses = grailsApplication.artefactHandlersByName["Controller"].artefactInfo.classes*.name
                    fullClassName = allClasses.find { String cls ->
                        cls.contains(".${controllerName}Controller")
                    }
                }
                params.each {
                    if (it.value instanceof String
                        && !it.key.equalsIgnoreCase("controller")
                        && !it.key.equalsIgnoreCase("action")
                        && fullClassName
                        && fullClassName.contains("net.biomodels.jummp")) {
                        String encodedValue = InputParameterSanitizer.encodeAsHTML(it.value)
                        params[it.key] = encodedValue
                    }
                }
            }
            after = { Map model ->

            }
            afterView = { Exception e ->

            }
        }
    }
}
