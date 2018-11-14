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

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification

@TestMixin(GrailsUnitTestMixin)
class ModelOfTheMonthTransportCommandSpec extends Specification {

    void "MoM command objects provide the expected URL"() {
        when: 'a MoM command object with an undefined date is created'
        def cmd = new ModelOfTheMonthTransportCommand()

        then: 'we fall back on the default BioModels MoM page URL'
        cmd.formattedURL == ModelOfTheMonthTransportCommand.FALLBACK_URL

        when: 'we create a MoM entry with a valid publication date'
        def today = new Date()
        cmd = new ModelOfTheMonth(publicationDate: today).toCommandObject()
        def year = today[Calendar.YEAR]
        def month = today[Calendar.MONTH] + 1 // Calendar.MONTH starts from 0
        month = month > 10 ? month.toString() : "0${month.toString()}"
        def expected = "${ModelOfTheMonthTransportCommand.URL_SEED}year=$year&month=$month"
        def url = cmd.formattedURL

        then: 'the command object has the expected access URL'
        url == expected
    }
}
