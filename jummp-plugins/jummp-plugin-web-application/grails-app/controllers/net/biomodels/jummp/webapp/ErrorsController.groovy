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





package net.biomodels.jummp.webapp
import net.biomodels.jummp.webapp.rest.errors.Error

import javax.servlet.http.HttpServletResponse

class ErrorsController {

	def springSecurityService
	def messageSource

	private Error getError(String code, Object[] args = null) {
		return new Error(messageSource.getMessage("error.${code}.title",args, Locale.getDefault()),
						 messageSource.getMessage("error.${code}.explanation",args, Locale.getDefault()))
	}

    def error403 = {
        response.setStatus HttpServletResponse.SC_FORBIDDEN
        if (params.format && params.format!="html") {
            respond getError("403")
        } else {
            [authenticated: springSecurityService.isLoggedIn()]
        }
    }

    def error404 = {
        if (params.format && params.format!="html") {
            respond getError("404", [request.forwardURI])
        } else {
            [resource: request.forwardURI]
        }
    }

    def error500 = {
        def exception = request.getAttribute('exception')
        String digest = ''
        if (exception) {
            if (exception.message) {
                digest = exception.message
            }
            exception.stackTrace.each {
                digest += it.toString()
            }
        }
        digest = digest.encodeAsMD5()
        if (params.format && params.format!="html") {
        	respond new Error("Internal Server Error", digest)
        } else {
            [code: digest]
        }
    }
}
