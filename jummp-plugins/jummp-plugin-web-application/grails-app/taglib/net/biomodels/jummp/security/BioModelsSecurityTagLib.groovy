/**
* Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
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





package net.biomodels.jummp.security

class BioModelsSecurityTagLib {
    static namespace = "bmsec"

    static defaultEncodeAs = [taglib:'html']

    def springSecurityService

    /**
     * Renders the body if the user is authenticated.
     */
    def whenLoggedIn = { attrs, body ->
        boolean otpValidated = session.enabled2FA == null
        if (springSecurityService.isLoggedIn() && otpValidated) {
            out << body()
        }
    }

    /**
     * Renders the body if the user is not authenticated.
     */
    def whenNotLoggedIn = { attrs, body ->
        boolean notLoggedIn = !springSecurityService.isLoggedIn() || session.enabled2FA
        if (notLoggedIn) {
            out << body()
        }
    }

}
