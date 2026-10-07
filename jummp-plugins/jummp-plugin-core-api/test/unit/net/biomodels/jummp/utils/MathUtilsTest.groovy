/**
 * Copyright (C) 2010-2024 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

package net.biomodels.jummp.utils

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotEquals
import static org.junit.Assert.assertTrue

class MathUtilsTest {

    // --- generatePassword ---

    @Test
    void testGeneratePassword_hasRequestedLengthAndUsesOnlyTheAlphabet() {
        String alphabet = "abc123"
        String password = MathUtils.generatePassword(alphabet, 200)
        assertEquals(200, password.length())
        assertTrue("only characters of the alphabet are used", password.every { alphabet.contains(it) })
    }

    @Test
    void testGeneratePassword_zeroLengthGivesEmptyString() {
        assertEquals("", MathUtils.generatePassword("abc", 0))
    }

    @Test
    void testGeneratePassword_usesTheWholeAlphabet() {
        String alphabet = "0123456789"
        Set<String> seen = MathUtils.generatePassword(alphabet, 1000).toList() as Set
        assertEquals(alphabet.toList() as Set, seen)
    }

    @Test
    void testGeneratePassword_consecutivePasswordsDiffer() {
        String alphabet = (('A'..'Z') + ('a'..'z') + ('0'..'9')).join()
        assertNotEquals(MathUtils.generatePassword(alphabet, 32), MathUtils.generatePassword(alphabet, 32))
    }

    // --- validUsername ---

    @Test
    void testValidUsername_typicalLowercaseUsername() {
        assertTrue("tnguyen should be valid", MathUtils.validUsername("tnguyen"))
    }

    @Test
    void testValidUsername_minimumLength() {
        assertTrue("4-character username should be valid", MathUtils.validUsername("abcd"))
    }

    @Test
    void testValidUsername_maximumLength() {
        String username = "a" * 64
        assertTrue("64-character username should be valid", MathUtils.validUsername(username))
    }

    @Test
    void testValidUsername_tooShort() {
        assertFalse("3-character username should be invalid", MathUtils.validUsername("abc"))
    }

    @Test
    void testValidUsername_tooLong() {
        String username = "a" * 65
        assertFalse("65-character username should be invalid", MathUtils.validUsername(username))
    }

    @Test
    void testValidUsername_allowedSpecialChars() {
        assertTrue("underscore allowed", MathUtils.validUsername("user_name"))
        assertTrue("dot allowed",        MathUtils.validUsername("user.name"))
        assertTrue("hyphen allowed",     MathUtils.validUsername("user-name"))
    }

    @Test
    void testValidUsername_mixedCaseAndDigits() {
        assertTrue("mixed case + digits should be valid", MathUtils.validUsername("User123"))
    }

    @Test
    void testValidUsername_disallowedChars() {
        assertFalse("space not allowed",         MathUtils.validUsername("user name"))
        assertFalse("@ not allowed",             MathUtils.validUsername("user@name"))
        assertFalse("slash not allowed",         MathUtils.validUsername("user/name"))
        assertFalse("accented char not allowed", MathUtils.validUsername("usérname"))
    }

    @Test
    void testValidUsername_emptyString() {
        assertFalse("empty string should be invalid", MathUtils.validUsername(""))
    }
}
