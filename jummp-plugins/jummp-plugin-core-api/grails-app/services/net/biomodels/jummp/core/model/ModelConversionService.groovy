/**
* Copyright (C) 2010-2019 EMBL-European Bioinformatics Institute (EMBL-EBI),
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





/**
 * @short Service class for handling the external model conversion service
 *
 * This service provides the high-level API to access external model conversion service.
 * The conversion service is running on another host. It is equipped API endpoints whereby
 * this service could communicate to get various exports of the given model.
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 *
 * @date 20180316
 */

package net.biomodels.jummp.core.model

import grails.util.Holders
import groovy.json.JsonException
import groovy.json.JsonSlurper
import net.biomodels.jummp.core.IModelConversionService
import net.biomodels.jummp.core.JummpException
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand as RFTC
import net.biomodels.jummp.core.model.RevisionTransportCommand as RTC
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.security.access.AccessDeniedException

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class ModelConversionService /*implements IModelConversionService*/ {

    private static final Log log = LogFactory.getLog(ModelConversionService.class)

    final String CONVERSION_SERVICE_URL = Holders.grailsApplication.config.jummp.model.converter.url

    final String EXPORT_FOLDER = Holders.grailsApplication.config.jummp.model.exportFolder

    def modelDelegateService

    Set<String> listOfFormatsSupportedExport() {
        // TODO: should retrieve from the external conversion service
        return ["SBML", "PharmML"]
    }

    Set<String> listOfFormatsSupportedForExport(String fromFormat) {
        // TODO: invoke the external service
        Set<String> formats
        switch (fromFormat.toLowerCase()) {
            case "sbml":
                formats = ["Octave", "XPP", "BioPAX"]
                break
            case "pharmml":
                formats = []
                break
            default:
                formats = []
                break
        }
        formats
    }

    boolean isSupportedForConversion(RTC revisionTC) {
        String format = revisionTC.format.identifier
        Set<String> supportedFormats = listOfFormatsSupportedForExport(format)
        supportedFormats.size() > 0
    }

    List<String> generateExports(String modelId, String revisionId) {
        RTC revisionTC
        try {
            revisionTC = modelDelegateService.getRevisionFromParams(modelId, revisionId)
        } catch(AccessDeniedException e) {
            log.error(e.message, e)
            return null
        } finally {
            return generateExports(revisionTC).collect {it.toString()}
        }
    }

    List<Path> generateExports(RTC revisionTC) {
        if (isSupportedForConversion(revisionTC)) {
            // Create the subfolder named revision_number under the model submission id folder
            final String MODEL_FOLDER = revisionTC.model?.submissionId
            File modelFolder = new File(EXPORT_FOLDER, MODEL_FOLDER)
            File revisionFolder = new File(modelFolder, revisionTC.revisionNumber.toString())
            if (!revisionFolder.exists()) {
                revisionFolder.mkdirs()
            }
            log.info("""\
Connecting conversion service to generate exports of the model ${revisionTC?.model?.submissionId}""")
            def mainFile = revisionTC.files.findAll {it.mainFile}
            // TODO: Make sure that the main file always presents and the model has only a main file
            mainFile = mainFile?.first()
            String format = revisionTC.format.identifier
            Set<String> supportedFormats = listOfFormatsSupportedForExport(format)
            List<Path> result = new ArrayList<Path>()
            supportedFormats.each {
                Path path = convertAndCache(mainFile, it, revisionTC, revisionFolder)
                if (path) {
                    result.add(path)
                }
            }
            return result
        } else {
            log.info("""\
The model ${revisionTC?.model?.submissionId} with the format ${revisionTC.format?.identifier} has not been supported for conversion yet""")
            return null
        }
    }

    private Path convertAndCache(RFTC mainFile, String toFormat,
                                 RTC revisionTC, File revisionFolder) {
        String fromFile = new File(mainFile.path).toURI()
        String params = "to=${toFormat.toLowerCase()}&file=${fromFile}"
        String command = "${CONVERSION_SERVICE_URL}${revisionTC.revisionNumber}?${params}"
        URL url = new URL(command)
        try {
            url = new URL(command)
        } catch (MalformedURLException e) {
            // TODO: throw a specific exception
            throw new JummpException("URL is malformed", e)
        } finally {
            log.info(url)
        }

        Object slurper = new JsonSlurper()
        try {
            slurper = new JsonSlurper().parse(url)
        } catch (JsonException e) {
            throw new JummpException("Could not parse model conversion information", e)
        } catch (Exception e) {
            throw new JummpException("Error retrieving model conversion information", e)
        } finally {
            log.info("Result: ${slurper["result"]}")
            // Copy the result (i.e. the file) to the model revision folder
            String filePath = slurper["result"]
            if (!filePath) {
                log.error("""\
There is an error while converting the model ${revisionTC.model.submissionId} to the format ${toFormat}""")
                return null
            }
            filePath = filePath.substring(5) // get rid of the prefix 'file:'
            File source = new File(filePath)
            String sourceFileName = filePath.substring(filePath.lastIndexOf(File.separator)+1)
            File target = new File(revisionFolder, sourceFileName)
            Path result = Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            return result
        }
        return null
    }
}
