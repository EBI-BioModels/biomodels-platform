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
 * JGit, Apache Commons, JUnit, Spring Security (or a modified version of that library), containing parts
 * covered by the terms of Common Public License, Apache License v2.0, Eclipse Distribution License v1.0, the licensors of this
 * Program grant you additional permission to convey the resulting work.
 * {Corresponding Source for a non-source form of such a combination shall
 * include the source code for the parts of JGit, Apache Commons, JUnit, Spring Security used as well as
 * that of the covered work.}
 **/


package net.biomodels.jummp.utils

import org.apache.commons.lang.RandomStringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.ThreadLocalRandom

class JummpUtils {

    public static final Logger LOGGER = LoggerFactory.getLogger(JummpUtils.class)

    static void sleep(int duration) {
        try{
            Thread.sleep(duration)
        } catch(InterruptedException ee) {
            Thread.currentThread().interrupt()
            LOGGER.error("Exception occurred during sleep, {}", ee)
            throw new RuntimeException("Exception occurred during sleep")
        }
    }

    /**
     * Random a sequence of string (include A-Z and 0-9) with a given length
     * @param length
     * @return
     */
    static String randStr(int length) {
        String charset = (('A'..'Z') + ('0'..'9')).join()
        return RandomStringUtils.random(length, charset.toCharArray())
    }

    /**
     * Random a number within a specific range
     * @param min
     * @param max
     * @return
     */
    static int randInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
