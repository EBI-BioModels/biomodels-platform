/**
 * Copyright (C) 2010-2019 EMBL-European Bioinformatics Institute (EMBL-EBI),
 * Deutsches Krebsforschungszentrum (DKFZ)
 *
 * This file is part of Jummp.
 *
 * Jummp is free software you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation either version 3 of the License, or (at your option) any
 * later version.
 *
 * Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along
 * with Jummp if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
 */

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
            return PROD_URL.diagramJSUrl
        } else {
            return DEV_URL.diagramJSUrl
        }
    }
}
