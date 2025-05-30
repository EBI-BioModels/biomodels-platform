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
 *
 * Additional permission under GNU Affero GPL version 3 section 7
 *
 * If you modify Jummp, or any covered work, by linking or combining it with
 * Apache Commons, Perf4j (or a modified version of that library), containing parts
 * covered by the terms of Apache License v2.0, the licensors of this
 * Program grant you additional permission to convey the resulting work.
 *{Corresponding Source for a non-source form of such a combination shall
 * include the source code for the parts of Apache Commons, Perf4j used as well as
 * that of the covered work.}
 **/

package net.biomodels.jummp.utils

import java.util.concurrent.ThreadLocalRandom

/**
 * Mathematical utilities are commonly used
 * @author Tung Nguyen, nvntung@gmail.com
 */
class MathUtils {
    enum PWD_HARD_LEVEL {
        EASY("Easy to guess."),
        MEDIUM("Medium difficulty."),
        HARD("Difficult."),
        X_HARD("Extremely difficult.")

        final String label

        PWD_HARD_LEVEL(String label) {
            this.label = label
        }

        String toString() {
            return this.label
        }
    }
    static final int rand(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    static final String generatePassword(String alphabet, int n) {
        new Random().with {
            (1..n).collect { alphabet[ nextInt( alphabet.length() ) ] }.join()
        }
    }

    static boolean isPositiveNumber(String value) {
        for (char c in value.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false
            }
        }
        return true
    }

    /**
     * Checks the strength of a given password
     * Source: https://martech.zone/javascript-password-strength/
     *
     * @param password
     * @return the difficulty level
     */
    static String checkPasswordStrength(String password) {
        // Initialize variables
        int strength = 0

        // Check password length
        if (password.length() < 8) {
            return PWD_HARD_LEVEL.EASY.label
        } else {
            strength += 1
        }

        // Check for mixed case
        if (password.matches(".*[a-z].*") && password.matches(".*[A-Z].*")) {
            strength += 1
        }

        // Check for numbers
        if (password.matches(".*\\d.*")) {
            strength += 1
        }

        // Check for special characters
        if (password.matches(".*[^a-zA-Z\\d].*")) {
            strength += 1
        }

        // Return strength level
        if (strength < 2) {
            return PWD_HARD_LEVEL.EASY.label
        } else if (strength == 2) {
            return PWD_HARD_LEVEL.MEDIUM.label
        } else if (strength == 3) {
            return PWD_HARD_LEVEL.HARD.label
        } else {
            return PWD_HARD_LEVEL.X_HARD.label
        }
    }
}
