/**
 * Copyright (C) 2010-2022 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
import static org.junit.Assert.assertTrue

/**
 * @author <a href="mailto:nvntung@gmail.com">Tung Nguyen</a> (https://ntung.github.io)
 * @created on 04/01/2022
 * @version 1.0
 */
class FileHelperTest {

    @Test
    void testAcceptableCharactersInFileName() {
        String fileName = "Zake2021_Metformin+Human+multiple+PO+dose.sedml"
        boolean expected = true
        boolean actual = FileHelper.isFileNameAcceptable(fileName)
        assertEquals(actual, expected)
        fileName = "Zake2021-Metformin-Human-multiple-PO-dose-à-æ_2.sedml"
        expected = false
        actual = FileHelper.isFileNameAcceptable(fileName)
        assertEquals(actual, expected)
        fileName = "Zake2021-Metformin-Human-multiple-PO-dose-2.sedml"
        expected = true
        actual = FileHelper.isFileNameAcceptable(fileName)
        assertEquals(actual, expected)
    }

    @Test
    void testFileNameHavingProperExtension() {
        String fileName = "Zake2021-Metformin-Human-multiple-PO-dose.xml"
        boolean actual = FileHelper.isFileNameAcceptable(fileName)
        assertTrue(actual)
    }

    @Test
    void testFileNameHavingMultipleDotSigns() {
        String fileName = "Zake2021-Metformin-Human-multiple-PO-dose.1.xml"
        boolean actual = FileHelper.isFileNameAcceptable(fileName)
        assertTrue(actual)
    }

    @Test
    void testFileNameNotHavingExtension() {
        String fileName = "Zake2021-Metformin-Human-multiple-PO-dose"
        boolean actual = FileHelper.isFileNameAcceptable(fileName)
        assertFalse(actual)
    }
}
