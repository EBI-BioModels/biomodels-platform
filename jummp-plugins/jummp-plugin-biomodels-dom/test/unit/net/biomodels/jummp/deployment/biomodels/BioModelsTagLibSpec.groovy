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
 */

package net.biomodels.jummp.deployment.biomodels

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.GroovyPageUnitTestMixin} for usage instructions
 */
@TestFor(BioModelsTagLib)
class BioModelsTagLibSpec extends Specification {
    final String DATE = '1970-01'
    final String EXPECTED_URL = "${ModelOfTheMonthTransportCommand.URL_SEED}$DATE"

    def setup() {
        def mockModelMonthService = mockFor(ModelOfTheMonthService)
        mockModelMonthService.demand.fetchEntriesForModel { Long id ->
            if (id == 0) { // model does not exist
                return []
            }
            [new ModelOfTheMonthTransportCommand(date: DATE, authors: 'Bob')]
        }
        tagLib.modelOfTheMonthService = mockModelMonthService.createMock()
    }

    void "test rendering of Model of the month information"() {
        when: 'we try to render the MoM entry for an undefined model'
        String result = tagLib.renderModelOfMonth(modelId: null).toString()

        then: 'the template is not rendered'
        !result

        when: 'we do not provide a modelId argument'
        result = tagLib.renderModelOfMonth().toString()

        then: 'the template is not rendered'
        !result

        when: 'we provide an id which does not exist'
        result = tagLib.renderModelOfMonth(modelId: 0).toString()

        then: 'the template is not rendered'
        !result

        when: 'we ask to render a MoM entry that exists'
        result = tagLib.renderModelOfMonth(modelId: 1)

        then: 'we get a valid response'
        result.contains EXPECTED_URL
    }
}
