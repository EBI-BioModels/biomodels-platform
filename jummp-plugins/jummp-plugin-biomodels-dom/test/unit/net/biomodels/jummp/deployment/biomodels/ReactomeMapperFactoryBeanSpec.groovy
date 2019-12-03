/*
    Copyright (C) 2019 EMBL-European Bioinformatics Institute (EMBL-EBI),
    Deutsches Krebsforschungszentrum (DKFZ)

This file is part of Jummp.

    Jummp is free software; you can redistribute it and/or modify it under the
terms of the GNU Affero General Public License as published by the Free
Software Foundation; either version 3 of the License, or (at your option) any
later version.

    Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
details.

    You should have received a copy of the GNU Affero General Public License along
with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.

Additional permission under GNU Affero GPL version 3 section 7

If you modify Jummp, or any covered work, by linking or combining it with
Apache Commons (or a modified version of that library), containing parts
covered by the terms of Apache License v2.0, the licensors of this
Program grant you additional permission to convey the resulting work.
    {Corresponding Source for a non-source form of such a combination shall include
        the source code for the parts of Apache Commons used as well as that of
        the covered work.}
*/

package net.biomodels.jummp.deployment.biomodels

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import net.biomodels.jummp.models.DefaultReactomeMapper
import net.biomodels.jummp.models.ReactomeMapperFactoryBean
import spock.lang.Specification

/**
 * @author carankalle on 26/11/2019.
 */
@TestMixin(GrailsUnitTestMixin)
class ReactomeMapperFactoryBeanSpec extends Specification {

    def "test extraction of reactome pathway mapped to models" () {
        defineBeans {
            defaultReactomeMapper(DefaultReactomeMapper) { bean ->
                bean.scope = 'prototype'
            }
        }
        ReactomeMapperFactoryBean reactomeFactory = new ReactomeMapperFactoryBean()
        reactomeFactory.setApplicationContext(applicationContext)

        when: 'we request reactome mapper bean'
        def reactomeMapper = reactomeFactory.getObject()
        then: 'reactome data extracted correctly'
        reactomeMapper.getModelPathwayMap().size() > 0
    }
}
