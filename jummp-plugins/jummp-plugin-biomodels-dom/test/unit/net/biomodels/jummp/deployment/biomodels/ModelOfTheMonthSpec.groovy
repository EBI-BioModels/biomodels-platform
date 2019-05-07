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
import grails.test.mixin.TestMixin
import grails.test.mixin.domain.DomainClassUnitTestMixin
import spock.lang.Specification

@TestMixin(DomainClassUnitTestMixin)
@TestFor(ModelOfTheMonth)
class ModelOfTheMonthSpec extends Specification {

    void "date formatting works when creating command objects"() {
        when: "the publication date is January 1970"
        def date = new Date(0)
        def mom = new ModelOfTheMonth(publicationDate: date)
        def cmd = mom.toCommandObject()

        then:
        cmd.formattedEntryDate == "1970-01"

        when: "the publication date is not set"
        mom.publicationDate = null
        cmd = mom.toCommandObject()

        then:
        null == cmd.formattedEntryDate
    }
}
