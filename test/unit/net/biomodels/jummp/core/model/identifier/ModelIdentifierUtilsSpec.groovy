/**
 * Copyright (C) 2010-2017 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

package net.biomodels.jummp.core.model.identifier

import grails.test.mixin.support.*
import net.biomodels.jummp.core.model.identifier.decorator.DateAppendingDecorator
import net.biomodels.jummp.core.model.identifier.decorator.FixedLiteralAppendingDecorator
import net.biomodels.jummp.core.model.identifier.decorator.VariableDigitAppendingDecorator
import net.biomodels.jummp.core.model.identifier.generator.AbstractModelIdentifierGenerator
import net.biomodels.jummp.core.model.identifier.generator.DefaultModelIdentifierGenerator
import net.biomodels.jummp.core.model.identifier.generator.NullModelIdentifierGenerator
import net.biomodels.jummp.core.model.identifier.support.GeneratorDetails
import org.junit.*

import static org.junit.Assert.*

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
class ModelIdentifierUtilsSpec {
    void testParseSettingsExpectsPartsToBeConsecutive() {
        def conf = '''
            part1 {
                type = "literal"
                suffix = "MODEL"
            }
            part2 {
                type = 'date'
                format = 'dd' // was suffix = 'dd'
            }
            part3 {
                type = 'numerical'
                width = 12
            }
        '''
        ConfigObject settings = new ConfigSlurper().parse(conf)
        try {
            ModelIdentifierUtils.buildDecoratorsFromSettings('random', settings)
            fail("The previous call should have thrown an exception.")
        } catch (Exception e) {
            String expected = "Please use strictly positive values in model ids."
            assertEquals(expected, e.message)
        }
    }

    void testParseSettingsExpectsPartsToStartFromOne() {
        ConfigObject settings = new ConfigObject()
        settings.part2.type = 'literal'
        settings.part2.suffix = 'MODEL'
        try {
            ModelIdentifierUtils.buildDecoratorsFromSettings("RANDOM", settings)
            fail("The previous call should have thrown an exception.")
        } catch (Exception e) {
            String expected = """\
Model id part order invalid: Expected part1, not part2. Please review the settings for jummp.model.id and ensure that the defined identifier parts are in consecutive order."""
            assertEquals(expected, e.message)
        }
    }

    void testParseSettingsPrunesInvalidPartTypes() {
        ConfigObject settings = new ConfigObject()
        settings.part1.type = 'unknown'
        settings.part1.suffix = 'MODEL'
        try {
            ModelIdentifierUtils.buildDecoratorsFromSettings('submission', settings)
            fail("The previous call should have thrown an exception.")
        } catch (Exception e) {
            String expected = "Unknown model id part type for part1: unknown"
            assertEquals(expected, e.message)
        }
    }

    void testParseSettingsCreatesTheCorrectDecorators() {
        def conf = '''
            part1 {
                type = "literal"
                suffix = "MODEL"
            }
            part2 {
                type = 'date'
                format = 'yyMMdd'
            }
            part3 {
                type = 'numerical'
                fixed = 'false'
                width = '12'
            }'''
        ConfigObject settings = new ConfigSlurper().parse(conf)
        GeneratorDetails results = ModelIdentifierUtils.buildDecoratorsFromSettings('submission', settings)
        assertNotNull results

        def actualDecorators = results.decorators.first().generator.getDecoratorRegistry()
        assertEquals 3, actualDecorators.size()
        def literalDecorator = ((FixedLiteralAppendingDecorator) actualDecorators.first())
        assertEquals 0, literalDecorator.ORDER
        assertEquals 'MODEL', literalDecorator.nextValue.get()
        def dateDecorator = actualDecorators.getAt(1) as DateAppendingDecorator
        assertEquals 1, dateDecorator.ORDER
        String FORMAT = 'yyMMdd'
        assertEquals FORMAT, dateDecorator.FORMAT
        assertEquals new Date().format(FORMAT), dateDecorator.nextValue.get()
        def numericalDecorator = actualDecorators.last() as VariableDigitAppendingDecorator
        assertEquals 2, numericalDecorator.ORDER
        final int WIDTH = 12
        assertEquals WIDTH, numericalDecorator.WIDTH
        assertEquals "0".padLeft(12, '0'), numericalDecorator.nextValue.get()
    }

    void testParseSettingsHasMandatoryConfigAttribute() {
        try {
            def results = ModelIdentifierUtils.buildDecoratorsFromSettings("", null)
            fail("I was expecting an exception to be thrown!!")
        } catch (Exception e) {
            assertTrue(e instanceof Exception)
            String firstLine = e.message.split(System.properties["line.separator"])[0]
            String expected = "The model identification scheme settings are not defined."
            assertTrue e.message.startsWith(expected)
        }
    }

    void testParseSettingsRejectsTrickyDateFormats() {
        def conf = '''
            part1 {
                type = "literal"
                suffix = "MODEL"
            }
            part2 {
                type = 'date'
                format = 'yy.MM ddZZ'
            }
            part3 {
                type = 'numerical'
                fixed = 'false'
                width = '12'
            }'''
        ConfigObject settings = new ConfigSlurper().parse(conf)
        try {
            ModelIdentifierUtils.buildDecoratorsFromSettings("submission", settings)
            fail("An exception should have been thrown by DateModelIdentifierPartition.")
        } catch (Exception e) {
            assertTrue e instanceof Exception
            String expected = """Date format yy.MM ddZZ is not appropriate. Try 'yyyyMMdd' or \
'yyMMdd'."""
            assertTrue e.message == expected
        }
    }

    void testParseSettingsRejectsTrickyLiteralSuffixFormats() {
        def conf = '''
            part1 {
                type = "literal"
                suffix = "S\tMILE"
            }
            part2 {
                type = 'date'
                format = 'yyMMdd'
            }
            part3 {
                type = 'numerical'
                fixed = 'false'
                width = '12'
            }'''
        ConfigObject settings = new ConfigSlurper().parse(conf)
        try {
            ModelIdentifierUtils.buildDecoratorsFromSettings('submission', settings)
            fail("An exception should have been thrown by LiteralModelIdentifierPartition.")
        } catch (Exception e) {
            assertTrue e instanceof Exception
            String expected = "Literal suffix S\tMILE is not valid."
            assertTrue e.message == expected
        }
    }

    void testParseSettingsCopesWithLongValues() {
        def conf = '''
            part1 {
                type = "literal"
                suffix = "MODEL"
            }
            part2 {
                type = 'numerical'
                fixed = 'false'
                width = '10'
            }'''
        ConfigObject settings = new ConfigSlurper().parse(conf)
        try {
            def result = ModelIdentifierUtils.buildDecoratorsFromSettings("submission",
                settings, "MODEL6687654321")
            assertNotNull result
            println result.properties
        } catch (Exception e) {
            fail("Should have not encountered an exception while processing MODEL6687654321: $e")
        }
    }
}
