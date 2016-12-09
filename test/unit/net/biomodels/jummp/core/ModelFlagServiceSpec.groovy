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

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import net.biomodels.jummp.model.Flag
import spock.lang.Specification

/**
 * @author  tung.nguyen@ebi.ac.uk
 * @author  mihai.glont@ebi.ac.uk
 */
@Mock(Flag)
@TestFor(ModelFlagService)
class ModelFlagServiceSpec extends Specification {
    def "create and save a flag"() {
        given: "arguments -- parameters"
        String label = "This is a valid SBML model"
        String description = "This model is satisfied with SBML specification"
        def icon = [1, 1, 1, 1, 1, 1, 1, 1, 1] as byte[]
        when: "insert the first flag"
        Flag result = service.saveFlag(label, description, icon)
        then: "should be fine"
        null != result
        when: "insert this record again"
        result = service.saveFlag(label, description, icon)
        then: "throw exception, error because of duplicated key"
        result == null
    }
}
