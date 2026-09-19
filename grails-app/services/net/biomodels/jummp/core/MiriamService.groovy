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
* groovy, Spring Framework, Perf4j, Spring Security (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0 the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of groovy, Spring Framework, Perf4j, Spring Security used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.core

import groovy.transform.PackageScope
import net.biomodels.jummp.core.miriam.IMiriamService
import org.springframework.beans.factory.InitializingBean
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.perf4j.aop.Profiled
import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler

import javax.xml.XMLConstants
import javax.xml.parsers.ParserConfigurationException
import javax.xml.parsers.SAXParserFactory
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Service for handling MIRIAM resources.
 *
 * This component fetches the export of the Identifiers.org registry and stores it
 * in the working directory.
 * @author Martin Gräßlin <m.graesslin@dkfz.de>
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
class MiriamService implements IMiriamService, InitializingBean {
    /**
     * Disable automatic transactional behaviour of Grails services.
     */
    static transactional = false
    /**
     * The class logger.
     */
    static final Log log = LogFactory.getLog(this.getClass())
    /**
     * Flag for logger verbosity.
     */
    static final boolean IS_INFO_ENABLED = log.isInfoEnabled()
    /**
     * The URL for obtaining the export from the identifiers.org registry.
     */
    final String DEFAULT_EXPORT_URL = "http://www.ebi.ac.uk/miriam/main/export/xml/"
    /**
     * The name of the file containing the export of the identifiers.org registry.
     */
    final String EXPORT_FILE_NAME = "miriam.xml"
    /**
     * How many redirects to follow to get to the export.
     */
    static final int MAX_REDIRECTS = 5
    static final int CONNECT_TIMEOUT_MILLIS = 15_000
    static final int READ_TIMEOUT_MILLIS = 60_000
    /**
     * The file containing the export of the identifiers.org registry.
     */
    File registryExport
    /**
     * Dependency injection of Grails Application.
     */
    @SuppressWarnings("GrailsStatelessService")
    def grailsApplication

    /**
     * Initialisation for the registry export instance variable.
     */
    void afterPropertiesSet() {
        String folderPath = grailsApplication.config.jummp.vcs.workingDirectory
        registryExport = new File(folderPath, EXPORT_FILE_NAME)
        log.info("Finished the bean initialisation")
    }

    @Profiled(tag="MiriamService.updateMiriamResources")
    public void updateMiriamResources(String url = DEFAULT_EXPORT_URL) {
        if (IS_INFO_ENABLED) {
            log.info "Started updating the identifiers.org registry export."
        }
        // Download next to the export and swap it in only once it is known to be one: writing straight to the export
        // truncated it before anything had been downloaded, so a failed refresh, or one that got an empty answer,
        // left an empty file for the search indexing to read (JBM-781).
        File download
        try {
            download = File.createTempFile(EXPORT_FILE_NAME, ".part", registryExport.absoluteFile.parentFile)
            download.withOutputStream { OutputStream out ->
                openRegistryExport(url).withStream { InputStream registry -> out << registry }
            }
            verifyRegistryExport(download)
            // a temporary file is only readable by its owner, an export written in place would not be
            download.setReadable(true, false)
            replaceRegistryExport(download)
            if (IS_INFO_ENABLED) {
                log.info "Finished updating the identifiers.org registry export."
            }
        } catch (IOException | SAXException | ParserConfigurationException e) {
            log.error("Cannot update identifiers.org registry from ${url}, keeping the existing export: " +
                "${e.message}", e)
        } finally {
            download?.delete()
        }
    }

    private void replaceRegistryExport(final File download) throws IOException {
        try {
            Files.move(download.toPath(), registryExport.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(download.toPath(), registryExport.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    /**
     * Checks that a download is a registry export: not empty, well-formed XML, and with a miriam element at the root.
     * The old export URL now leads to the home page of identifiers.org, which is none of these.
     */
    @PackageScope
    static void verifyRegistryExport(final File download)
            throws IOException, SAXException, ParserConfigurationException {
        if (download.length() == 0) {
            throw new IOException("The download is empty")
        }
        SAXParserFactory factory = SAXParserFactory.newInstance()
        factory.setNamespaceAware(true)
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        List<String> rootElement = []
        factory.newSAXParser().parse(download, new DefaultHandler() {
            @Override
            void startElement(String uri, String localName, String qName, Attributes attributes) {
                if (rootElement.isEmpty()) {
                    rootElement << localName
                }
            }
        })
        if (rootElement != ["miriam"]) {
            throw new IOException("The download is not a registry export, its root element is ${rootElement}")
        }
    }

    /**
     * Opens the stream the registry export is downloaded from. This is the only place that reaches the network, so
     * that tests can stub it and never depend on (or hammer) the identifiers.org registry.
     *
     * Unlike URL.openStream() it follows redirects from http to https (which HttpURLConnection never does) and
     * reports any answer other than 200 as an error, instead of handing back an empty stream.
     */
    // @PackageScope (not private) so tests can stub it via metaClass - see PubMedService.lookupPublicationDataInPubMed
    @PackageScope
    InputStream openRegistryExport(final String url) throws IOException {
        URL current = new URL(url)
        for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
            if (!(current.protocol in ["http", "https"])) {
                throw new IOException("Cannot download the registry export from ${current}: only http and https " +
                    "addresses are supported")
            }
            HttpURLConnection connection = (HttpURLConnection) current.openConnection()
            connection.instanceFollowRedirects = false
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            int status = connection.responseCode
            if (status == HttpURLConnection.HTTP_OK) {
                return connection.inputStream
            }
            String location = connection.getHeaderField("Location")
            connection.disconnect()
            if (status in [301, 302, 303, 307, 308]) {
                if (!location) {
                    throw new IOException("${current} answered HTTP ${status} without a Location to redirect to")
                }
                current = new URL(current, location)
            } else {
                throw new IOException("${current} answered HTTP ${status}")
            }
        }
        throw new IOException("Gave up on ${url} after ${MAX_REDIRECTS} redirects")
    }
}
