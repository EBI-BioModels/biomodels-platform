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
* Apache Commons, Spring Framework (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Apache Commons, Spring Framework used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.webapp

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.web.multipart.MultipartFile

/**
 * Command object for validating the files supplied by the user as part of a
 * model submission.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 * @date 20130701
 */
@grails.validation.Validateable
class UploadFilesCommand implements Serializable {
    private static final long serialVersionUID = 1L
    private static final Log log = LogFactory.getLog(this)
    private static final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()
    private static final boolean IS_INFO_ENABLED  = log.isInfoEnabled()

    /**
     * The main files of the model
     */
    List<MultipartFile> mainFile
    /**
     * The descriptions associated with the main files
     */
    List<String> mainFileDescription
    /**
     * The additional files belonging to a model.
     */
     List<MultipartFile> extraFiles
     /**
      * The descriptions associated with each additional file
      */
     List<String> description
     /**
      * The list of main files to be deleted
      */
     List<String> mainDeletes
     /**
      * The list of additional files to be deleted
      */
     List<String> extraDeletes

    /**
     * Provides the additional files along with their descriptions in a map.
     *
     * @return a Map with entries of type Map.MapEntry<MultipartFile, String>.
     */
    public Map<MultipartFile, String> additionalFilesAsMap() {
        if (IS_DEBUG_ENABLED) {
            log.debug(String.format("Providing %s additional files as a map", extraFiles.size()))
        }
        def result = [:]
        extraFiles?.eachWithIndex { f, i ->
            if (description && description[i]) {
                result[f] = description[i]
            } else {
                result[f] = ""
            }
        }
        result
    }

    public static boolean containsDuplicates(List<MultipartFile> files) {
        if (!files) {
            // deal with null and empty elsewhere
            return false
        } else {
            def fileProperties = []
            files.each { it ->
                final def currentFileProperties = [
                    it.getOriginalFilename(),
                    it.getSize(),
                    it.getContentType()
                ]
                if (fileProperties.contains(currentFileProperties)) {
                    return true
                } else {
                    fileProperties << currentFileProperties
                }
            }
            return false
        }
    }

    static constraints = {
        mainFile(nullable: false,
            validator: { mf ->
                if (IS_INFO_ENABLED) {
                    log.info("\nSubmission started. Inspecting ${mf.inspect()}")
                }
                if (mf.findAll {!it || it.isEmpty()}.size() > 0 ){
                    if (IS_DEBUG_ENABLED) {
                        log.debug("Some main file fields were blank. Current main file(s): ${mf.inspect()}.")
                    }
                }
                mf = mf.findAll{ it && !it.isEmpty() }
                if (mf.size() < 1) {
                    log.warn "\tPlease give me some files."
                    return ['mainFile.blank']
                }
                //process mains in reverse so that the latest submitted one is kept.
                // unique accepts a comparator closure that returns -1,0,1, but we only care about equality
                mf = mf.reverse().unique { self, other ->
                    self.getOriginalFilename().equals(other.getOriginalFilename()) ? 0 :
                        self.getSize() > other.getSize() ? -1 : 1
                }
                return true
            }
        )
        extraFiles(nullable: true,
            validator: { supplements, cmd ->
                if (!supplements) {
                    if (IS_INFO_ENABLED) {
                        log.info "No additional files were provided for ${cmd.inspect()}."
                        return true
                    }
                }
                if (IS_DEBUG_ENABLED) {
                    log.debug("Validating additional files ${supplements.inspect()}.")
                }
                // purge empty files
                supplements = supplements.findAll {it && !it.isEmpty()}
                int supplementCount = supplements.size()
                if (IS_DEBUG_ENABLED) {
                    log.debug(String.format("There are %s supplementary files in this submission: %s.",
                            supplementCount, supplements.inspect()))
                }
                if (supplementCount > 0) {
                    //discard files with the same name while keeping the most recent entries
                    def duplicateList = []
                    def additionalFilesMap = cmd.additionalFilesAsMap()
                    additionalFilesMap.keySet().toList().reverse().unique { self, other ->
                        if(self.getOriginalFilename().equals(other.getOriginalFilename())) {
                            duplicateList << self
                            return 0
                        }
                        //ignore order if file names are unique
                        return -1
                    }
                    additionalFilesMap.additionalFilesMap.findAll { !duplicateList.contains(it.key) }
                    supplements = additionalFilesMap.keySet()
                    // the descriptions of the duplicates are not of interest to us
                    cmd.description = additionalFilesMap.values() as List

                    //now ensure that no additional file has the same name as the main
                    duplicateList = []
                    def mainNamesList = []
                    cmd.mainFile.each { mainNamesList << it.getOriginalFilename() }
                    supplements = supplements.findAll { !mainNamesList.contains(it.getOriginalFilename()) }

                    //finally, log which additional files do not have a description
                    supplements.eachWithIndex{ file, i ->
                        if (!cmd.description[i]) {
                            if (IS_DEBUG_ENABLED) {
                                log.debug(String.format("Supplementary file %s does not have a description.",
                                            file.getOriginalFilename()))
                            }
                        }
                    }
                }
                //almost done, log the end result
                if (IS_INFO_ENABLED) {
                    StringBuilder sb = new StringBuilder("\nData binding for submission complete.")
                    sb.append("\nMain file:").append(cmd.mainFile.inspect()).append("\nAdditional files:\n")
                    cmd.additionalFilesAsMap().each {
                        String name = (it.key as MultipartFile).getOriginalFilename()
                        sb.append(name).append(":").append(it.value).append("\n")
                    }

                    log.info sb.toString()
                }
                return true
            }
        )
        mainFileDescription(nullable: false)
        description(nullable: false)
        mainDeletes(nullable: true)
        extraDeletes(nullable: true)
    }
}
