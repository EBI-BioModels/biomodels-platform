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
 **/

package net.biomodels.jummp.deployment.biomodels

import grails.plugin.springsecurity.annotation.Secured

/**
 * This controller aims to serve special features
 */
@Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
class FeatureController {
    def featureService

    def agedbrain() {
    }

    def path2models() {

    }

    def covid19() {
        [content: featureService.covid19PageContent]
    }

    def reproducibility() {
        [content: featureService.loadContentForReproducibilityPage]
    }
}
