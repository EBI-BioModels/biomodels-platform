/**
 * Copyright (C) 2010-2021 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern

class FileHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileHelper.class)

    private static final Pattern acceptableCharactersInFileName = ~/^[a-zA-Z0-9\s_\.\+-]+\.[\w]+$/

    static boolean isFileNameAcceptable(final String filename) {
        if (!filename) { return false }
        Matcher matcher = filename =~ acceptableCharactersInFileName
        boolean retVal = matcher.matches()
        retVal
    }

    static void writeUsingFiles(String data, String absFilePath) {
        try {
            Files.write(Paths.get(absFilePath), data.getBytes())
        } catch (IOException e) {
            LOGGER.error("Errors occurred when writing {} to the file {} due to the cause: {}.", data, absFilePath, e.getMessage())
        }
    }

    static File createFile(String parentDir, String filename) {
        Path dirPath = Paths.get(parentDir)
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath)
        }

        Path filePath = dirPath.resolve(filename);
        if (!Files.exists(filePath)) {
            Files.createFile(filePath)
        }

        File file = new File(parentDir, filename);
        if (file.exists()) {
            LOGGER.info("File {} has been created.", file.absolutePath)
        } else {
            LOGGER.info("Errors occurred when creating the file {}.", file.absolutePath)
        }
        file
    }
}
