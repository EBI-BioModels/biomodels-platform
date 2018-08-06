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

import static org.junit.Assert.*

import grails.test.mixin.*
import grails.test.mixin.support.*
import net.biomodels.jummp.core.model.identifier.decorator.DateAppendingDecorator
import net.biomodels.jummp.core.model.identifier.decorator.FixedLiteralAppendingDecorator
import net.biomodels.jummp.core.model.identifier.decorator.VariableDigitAppendingDecorator
import net.biomodels.jummp.core.model.identifier.generator.DefaultModelIdentifierGenerator
import net.biomodels.jummp.core.model.identifier.generator.NullModelIdentifierGenerator
import org.junit.*

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
class ModelIdentifierUtilsSpec {
    Set<String> actualIdentifierSchemeRegexes

    @Before
    void setUp() {
        actualIdentifierSchemeRegexes = ModelIdentifierUtils.MODEL_ID_REGEXES
        ModelIdentifierUtils.MODEL_ID_REGEXES.clear()
    }

    @After
    void tearDown() {
        ModelIdentifierUtils.MODEL_ID_REGEXES.addAll(actualIdentifierSchemeRegexes)
    }

    void testParseSettingsExpectsPartsToBeConsecutive() {
        def conf = '''
            model {
                submission {
                    id {
                        part1 {
                            type = "literal"
                            suffix = "MODEL"
                        }
                        part2 {
                            type = 'date'
                            suffix = 'dd'
                        }
                        part3 {
                            type = 'numerical'
                            width = 12
                        }
                    }
                }
            }
            database {
                    username = 'sa'
                    password = ''
                    type = 'h2'
                    // fall back to an in-memory H2 database instance
                }
        '''
        ConfigObject settings = new ConfigSlurper().parse(conf)
        try {
            ModelIdentifierUtils.processGeneratorSettings(settings)
            fail("The previous call should have thrown an exception.")
        } catch (Exception e) {
            String expected = """\
The configuration settings lack the rules for generating model identifiers!"""
            assertEquals(expected, e.message)
        }
    }

    void testParseSettingsExpectsPartsToStartFromOne() {
        ConfigObject settings = new ConfigObject()
        settings.model.id.submission.part2.type = 'literal'
        settings.model.id.submission.part2.suffix = 'MODEL'
        settings.database.username = 'jummp'
        settings.database.password = 'jummpHigher'
        settings.database.server = 'localhost'
        settings.database.port = '3306'
        settings.database.database = 'ddmore-live'
        settings.database.type = null
        try {
            ModelIdentifierUtils.processGeneratorSettings(settings)
            fail("The previous call should have thrown an exception.")
        } catch (Exception e) {
            String expected = """\
Model id part order invalid: Expected part1, not part2. Please review the settings for jummp.model.id and ensure that the defined identifier parts are in consecutive order."""
            assertEquals(expected, e.message)
        }
    }

    void testParseSettingsPrunesInvalidPartTypes() {
        ConfigObject settings = new ConfigObject()
        settings.model.id.submission.part1.type = 'unknown'
        settings.model.id.submission.part1.suffix = 'MODEL'
        settings.database.username = 'jummp'
        settings.database.password = 'jummpHigher'
        settings.database.server = 'localhost'
        settings.database.port = '3306'
        settings.database.database = 'ddmore-live'
        settings.database.type = null
        try {
            ModelIdentifierUtils.processGeneratorSettings(settings)
            fail("The previous call should have thrown an exception.")
        } catch (Exception e) {
            String expected = "Unknown model id part type for part1: unknown"
            assertEquals(expected, e.message)
        }
    }

    void testParseSettingsCreatesTheCorrectDecorators() {
        def conf = '''
            model {
                id {
                    submission {
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
                        }
                    }
                }
            }
            database {
                username = 'sa'
                password = ''
                type = 'h2'
                // fall back to an in-memory H2 database instance
            }'''
        ConfigObject settings = new ConfigSlurper().parse(conf)
        def results = ModelIdentifierUtils.processGeneratorSettings(settings)
        assertNotNull results
        def generators = ['submissionIdGenerator' : DefaultModelIdentifierGenerator.class,
                          'publicationIdGenerator': NullModelIdentifierGenerator.class
        ]
        assertEquals generators.keySet(), results.keySet()
        assertEquals generators.size(), results.size()
        results.each { name, generator ->
            Class clazz = generators[name]
            assertEquals clazz, generator.getClass()
        }
        def actualDecorators = results.values().toList().first().DECORATOR_REGISTRY
        assertEquals 3, actualDecorators.size()
        def literalDecorator = actualDecorators.first()
        assertTrue literalDecorator instanceof FixedLiteralAppendingDecorator
        assertEquals 0, literalDecorator.ORDER
        assertEquals 'MODEL', literalDecorator.nextValue.get()
        def dateDecorator = actualDecorators.getAt(1)
        assertTrue dateDecorator instanceof DateAppendingDecorator
        assertEquals 1, dateDecorator.ORDER
        String FORMAT = 'yyMMdd'
        assertEquals FORMAT, dateDecorator.FORMAT
        assertEquals new Date().format(FORMAT), dateDecorator.nextValue.get()
        def numericalDecorator = actualDecorators.last()
        assertTrue numericalDecorator instanceof VariableDigitAppendingDecorator
        assertEquals 2, numericalDecorator.ORDER
        final int WIDTH = 12
        assertEquals WIDTH, numericalDecorator.WIDTH
        assertEquals "0".padLeft(12, '0'), numericalDecorator.nextValue.get()
        assertEquals 1, ModelIdentifierUtils.MODEL_ID_REGEXES.size()
        assertEquals "\\QMODEL\\E\\d{2}?\\d{2}?\\d{2}?\\d{12}?",
            ModelIdentifierUtils.MODEL_ID_REGEXES.first()

        ModelIdentifierUtils.MODEL_ID_REGEXES.add("BIOMD\\d{10}")
        String p = ModelIdentifierUtils.MODEL_ID_REGEXES.join('|')
        assertTrue "BIOMD0000000001".matches(p)
        assertTrue "MODEL987789123456123456".matches(p)
    }

    void testParseSettingsHasMandatoryConfigAttribute() {
        try {
            def results = ModelIdentifierUtils.processGeneratorSettings(null)
            fail("I was expecting an exception to be thrown!!")
        } catch (Exception e) {
            assertTrue(e instanceof Exception)
            String firstLine = e.message.split(System.properties["line.separator"])[0]
            String expected = "The settings for the model identification scheme are missing."
            assertTrue e.message.startsWith(expected)
        }
    }

    void testParseSettingsRejectsTrickyDateFormats() {
        def conf = '''
            model {
                id {
                    submission {
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
                        }
                    }
                }
            }
            database {
                username = 'sa'
                password = ''
                type = 'h2'
                // fall back to an in-memory H2 database instance
            }'''
        ConfigObject settings = new ConfigSlurper().parse(conf)
        try {
            ModelIdentifierUtils.processGeneratorSettings(settings)
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
            model {
                id {
                    submission {
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
                        }
                    }
                }
            }
            database {
                username = 'sa'
                password = ''
                type = 'h2'
                // fall back to an in-memory H2 database instance
            }'''
        ConfigObject settings = new ConfigSlurper().parse(conf)
        try {
            ModelIdentifierUtils.processGeneratorSettings(settings)
            fail("An exception should have been thrown by LiteralModelIdentifierPartition.")
        } catch (Exception e) {
            assertTrue e instanceof Exception
            String expected = "Literal suffix S\tMILE is not valid."
            assertTrue e.message == expected
        }
    }

    void testParseSettingsCopesWithLongValues() {
        def conf = '''
            model {
                id {
                    submission {
                        part1 {
                            type = "literal"
                            suffix = "MODEL"
                        }
                        part2 {
                            type = 'numerical'
                            fixed = 'false'
                            width = '10'
                        }
                    }
                }
            }
            database {
                username = 'sa'
                password = ''
                type = 'h2'
                // fall back to an in-memory H2 database instance
            }'''
        ConfigObject settings = new ConfigSlurper().parse(conf)
        ConfigObject submissionSettings = settings.model.id.submission
        try {
            def result = ModelIdentifierUtils.buildDecoratorsFromSettings(submissionSettings,
                "MODEL6687654321")
            assertNotNull result
            println result.properties
        } catch (Exception e) {
            fail("Should have not encountered an exception while processing MODEL6687654321: $e")
        }
    }

    void testExplicitSettingOfModelIdentifierRegex() {
        def conf = '''
            model {
                id {
                    submission {
                        part1 {
                            type = "literal"
                            suffix = "MYMODEL"
                        }
                        part2 {
                            type = "numerical"
                            fixed = "false"
                            width = "22"
                        }
                    }
                    regex = "MYMODEL\\\\d{22}"
                }
            }
            database {
                username = "sa"
                password = ""
                type = "h2"
                // fall back to an in-memory H2 database instance
            }
        '''
        ConfigObject config = new ConfigSlurper().parse(conf)
        ModelIdentifierUtils.processGeneratorSettings(config)
        assertEquals 1, ModelIdentifierUtils.MODEL_ID_REGEXES.size()
        assertEquals "MYMODEL\\d{22}", ModelIdentifierUtils.MODEL_ID_REGEXES.first()
    }

    @Test(expected = IllegalArgumentException)
    void testInvalidRegexSetting() {
        def conf = '''
            model {
                id {
                    submission {
                        part1 {
                            type = "literal"
                            suffix = "ABC"
                        }
                        part2 {
                            type = "numerical"
                            fixed = "false"
                            width = "2"
                        }
                    }
                    regex = "\\\\"
                }
            }
            database {
                username = "sa"
                password = ""
                type = "h2"
                // fall back to an in-memory H2 database instance
            }
        '''
        ConfigObject config = new ConfigSlurper().parse(conf)
        ModelIdentifierUtils.processGeneratorSettings(config)
    }
}
