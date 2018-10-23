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

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

import net.biomodels.jummp.core.util.JummpHttpService

class ModelConversionService implements IModelConversionService {

    private static final Log log = LogFactory.getLog(ModelConversionService.class)

    def grailsApplication = Holders.grailsApplication

    final String CONVERSION_SERVICE_URL = grailsApplication.config.jummp.model.converter.url

    final String EXPORT_FOLDER = grailsApplication.config.jummp.model.exportFolder

    def repositoryFileService

    /**
     * This utility method aims to check the connection state of the external conversion service.
     * It will return true if the service is till alive. Otherwise, the method returns false.
     *
     * @return  true/false
     */
    boolean isAlive() {
        String infoFormat = "${CONVERSION_SERVICE_URL}info/mapSupportedFormats"
        isAlive(infoFormat)
    }

    boolean isAlive(String url) {
        200 == JummpHttpService.getStatusCode(url)
    }

    String getConversionServiceEndpoint() {
        CONVERSION_SERVICE_URL
    }
    /**
     * This method returns the list of model formats that Conversion Service
     * currently supports for converting the model under to a given format to
     * these supported formats.
     *
     * @param   fromFormat A string representing a specific model format
     * @return  a set      The list of strings which are formats supported by Conversion Service
     */
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

    /**
     * This methods determines whether a given model revision will be supported
     * for the conversion or not
     *
     * @param revisionTC    A revision transport command objects representing
     *                      the model which will be converted
     * @return a boolean    True/False
     */
    boolean isSupportedForConversion(RTC revisionTC) {
        String format = revisionTC.format.identifier
        Set<String> supportedFormats = listOfFormatsSupportedForExport(format)
        supportedFormats.size() > 0
    }

    /**
     * This method allows to generate exports of a given model revision to all supported formats.
     * Based on the list of supported formats by Conversion Service, this method will try to convert
     * the model to each formats and store the converted file into the location externalised in the
     * application settings.
     *
     * @param revisionTC    A revision transport command objects representing
     *                      the model which will be converted
     * @return a list       The list of Paths which files are converted ones
     */
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
            List mainFiles = revisionTC.files.findAll {it.mainFile}
            // TODO: Make sure that the main file always presents and the model has only a main file
            RFTC mainFile = mainFiles?.first()
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

    /**
     * This method aims to convert and store the result in the externalised location
     *
     * @param mainFile      A repository file transport command representing the main file
     * @param toFormat      A string representing the output format
     * @param revisionTC    A revision transport command representing the model revision
     * @param revisionFolder A file object denoting the location whereby the converted will be stored
     * @return a Path object A Path object showing the location where the file presents.
     */
    private Path convertAndCache(RFTC mainFile, String toFormat,
                                 RTC revisionTC, File revisionFolder) {
        String fromFile = new File(mainFile.path).toURI()
        String params = "to=${toFormat.toLowerCase()}&file=${fromFile}"
        String request = "${CONVERSION_SERVICE_URL}converter/convert/${revisionTC.revisionNumber}?${params}"
        Object data = fetchDataFromConversionService(request)
        if (data) {
            log.info("Result: ${data["result"]}")
            // Copy the result (i.e. the file) to the model revision folder
            String filePath = data["result"]
            if (!filePath) {
                log.error("""\
There is an error while converting the model ${revisionTC.model.submissionId} to the format ${toFormat}""")
                return null
            }
            Path source = Paths.get(new URI(filePath))
            String fileName = source.getFileName()
            Path target = Paths.get(revisionFolder.getAbsolutePath(), fileName)
            Path result = Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
            return result
        }
        return null
    }

    /**
     * This method enables us to retrieve the converted files of a given revision
     *
     * @param revisionTC    The revision transport command represents the target revision
     * @return a list       The list of files converted from the revision's format to the others
     */
    List<RFTC> getConvertedFiles(RTC revisionTC) {
        final String MODEL_FOLDER = revisionTC.model?.submissionId
        String modelFolder = "${EXPORT_FOLDER}${File.separator}${MODEL_FOLDER}"
        File revisionFolder = new File(modelFolder, revisionTC.revisionNumber.toString())
        if (revisionFolder.exists()) {
            List<File> files = revisionFolder.listFiles()
            List<RFTC> fileTCs = repositoryFileService.asRFTCList(files)
            // The RFTC objects have been already initialised three attributes.
            // We just need to update the remaining attributes
            Map<String, String> mapFormats = getSupportedFormats()
            fileTCs.each {
                String fileExtension = extractFileExtension(it.path)
                String fileFormatIdentifier = mapFormats.get(fileExtension)
                it.mimeType = fileFormatIdentifier ?: "UNKNOWN"
                it.mainFile = false
                it.hidden = false
                it.userSubmitted = false
                it.revision = revisionTC
            }
            return fileTCs
        }
        return null
    }

    private Map<String, String> getSupportedFormats() {
        String request = "${CONVERSION_SERVICE_URL}info/mapSupportedFormats"
        isAlive(request as String)
        Object data = fetchDataFromConversionService(request)
        Map result = new HashMap()
        if (data != null) {
            data.each {
                result.put(it.key.substring(1), it.value)
            }
        } else {
            // return the default values what are the current formats supported by Conversion Service
            result.put("m", "Octave")
            result.put("owl", "BioPAX")
            result.put("xpp", "XPP")
        }
        return result
    }

    private fetchDataFromConversionService(String request) {
        URL url
        try {
            url = new URL(request)
            log.info(url)
            Object slurper = new JsonSlurper()
            try {
                slurper = new JsonSlurper().parse(url)
            } catch (JsonException e) {
                slurper = null
                throw new JummpException("Could not parse model conversion information", e)
            } catch (Exception e) {
                slurper = null
                throw new JummpException("Error retrieving model conversion information", e)
            } finally {
                return slurper
            }
        } catch (MalformedURLException e) {
            // TODO: throw a specific exception
            throw new JummpException("URL is malformed", e)
        } finally {
            log.debug("The conversion request has been finished!")
        }
    }

    private String extractFileExtension(String fileName) {
        fileName.substring(fileName.lastIndexOf(".")+1)
    }
}
