/**
 * Copyright (C) 2010-2019 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 *
 * @testers     Tung Nguyen <tnguyen@ebi.ac.uk>
 */
@TestFor(P2MMapping)
class P2MMappingSpec extends Specification {
    String representative
    String member

    def setup() {
        representative = "BMID000000112901"
        member = "BMID000000112902"
    }

    void "test the number of records at initial stage"() {
        expect: "the table has nothing"
        0 == P2MMapping.count
    }

    void "test saving data into the table"() {
        when: "saving a mapping into the database"
        def map = new P2MMapping(representative: representative, member: member)
        map.save()

        then: "the count should be positive"
        P2MMapping.count
    }
}
