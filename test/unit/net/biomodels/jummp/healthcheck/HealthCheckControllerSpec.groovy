/*
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

package net.biomodels.jummp.healthcheck

import grails.test.mixin.TestFor
import spock.lang.Specification

@TestFor(HealthCheckController)
class HealthCheckControllerSpec extends Specification {
    def "should return a health check status"() {
        given:
        def service = mockFor(HealthCheckService)
        service.demand.getStatus { ->
            new HealthCheck(type: HealthCheck.TYPE.DATABASE, status: HealthCheck.STATUS.UNKNOWN)
        }
        controller.healthCheckService = service.createMock()
        HealthCheckUtil.registerObjectMarshaller()

        when:
        controller.status()

        then:
        response.text == '[{"database":"unknown"}]'
    }
}
