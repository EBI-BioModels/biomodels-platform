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
*
* Additional permission under GNU Affero GPL version 3 section 7
*
* If you modify Jummp, or any covered work, by linking or combining it with
* Grails, JUnit (or a modified version of that library), containing parts
* covered by the terms of Common Public License, Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Grails, JUnit used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.core

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import net.biomodels.jummp.Feedback
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
/**
 * @author  Tung Nguyen <tung.nguyen@ebi.ac.uk>
 * @date    14.09.2017
 */
@Mock(Feedback)
@TestFor(FeedbackService)
class FeedbackServiceSpec extends Specification {
    void "create and save a feedback"() {
        given: "arguments -- parameters"
            byte star = 5
            String email = "user@test.com"
            String comment = "This site is very interesting"
        when: "insert the first feedback"
            boolean result = service.persist(star, email, comment)
        then: "should be fine"
            result
        when: "insert this record again"
            result = service.persist(star, email, comment)
        then: "throw exception, error because of duplicated star and email"
            !result
    }
}
