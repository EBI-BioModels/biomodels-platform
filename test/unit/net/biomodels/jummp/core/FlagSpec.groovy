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
 **/





package net.biomodels.jummp.core

import grails.test.mixin.TestFor
import net.biomodels.jummp.model.Flag
import spock.lang.Specification

/**
 * @author  tung.nguyen@ebi.ac.uk
 * @date    06.12.2016
 */
@TestFor(Flag)
class FlagSpec extends Specification {
    void "test count flag"() {
        expect: "Test execute Hibernate count query"
        Flag.count() == 0
    }

    void "test create a Flag object and validate it"() {
        given: "an Flag object"
        String label = "This is a valid PharmML model"
        String description = "Model has been validated in PharmML specification"
        Flag f = new Flag(label: label, description: description)
        when: "call validate method"
        f.validate()
        then: "hope to have issues because of missing icon parameter"
        f.hasErrors() == true
    }

    void "test create and save a flag"() {
        given: "an Flag object"
            String label = "This is a valid PharmML model"
            String description = "Model has been validated in PharmML specification"
            byte[] icon = [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]
            Flag f = new Flag(label: label, description: description, icon: icon)
        when: "attempt to save it"
            f.save()
        then: "hope no error"
            f.hasErrors() == false
    }
}
