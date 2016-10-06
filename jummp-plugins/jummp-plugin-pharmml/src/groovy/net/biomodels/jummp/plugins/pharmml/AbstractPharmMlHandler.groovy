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
* Apache Tika, Apache Commons, LibPharmml, Perf4j (or a modified version of these
* libraries), containing parts covered by the terms of Apache License v2.0,
* the licensors of this Program grant you additional permission to convey the
* resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Apache Tika, Apache Commons,
* LibPharmml, Perf4j used as well as that of the covered work.}
**/

package net.biomodels.jummp.plugins.pharmml

import eu.ddmore.libpharmml.IPharmMLResource
import eu.ddmore.libpharmml.PharmMlFactory
import eu.ddmore.libpharmml.dom.PharmML
import eu.ddmore.libpharmml.impl.LibPharmMLImpl
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicBoolean
import net.biomodels.jummp.core.IPharmMlService
import net.biomodels.jummp.core.model.RevisionTransportCommand
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.perf4j.aop.Profiled

abstract class AbstractPharmMlHandler implements IPharmMlService {
    /* the class logger */
    private static final Log log = LogFactory.getLog(this)
    private static final boolean IS_INFO_ENABLED = log.isInfoEnabled()

    static PharmML getDomFromRevision(RevisionTransportCommand revision) {
        IPharmMLResource resource = getResourceFromRevision(revision)
        return resource?.dom
    }

    static IPharmMLResource getResourceFromRevision(RevisionTransportCommand revision) {
        if (!revision) {
            log.error "Cannot get PharmML DOM from an undefined revision."
            return ""
        }
        assert revision.format.identifier == "PharmML"
        List<File> revisionFiles = fetchMainFilesFromRevision(revision)
        final File pharmML = findPharmML(revisionFiles)
        return getResourceFromPharmML(pharmML)
    }

    @Profiled(tag="abstractPharmMlHandler.getDomFromPharmML")
    static PharmML getDomFromPharmML(File f) {
        IPharmMLResource resource = getResourceFromPharmML(f)
        return resource?.getDom()
    }

    /*
     * Helper function that finds the PharmML file from a selection of files.
     *
     * @param  submission the list of files containing a PharmML file.
     * @return the PharmML file, or null if @p submission had no PharmML files.In the case of
     * multiple PharmML files being present, only the first one is returned.
     */
    @Profiled(tag="abstractPharmMlHandler.findPharmML")
    static File findPharmML(List<File> submission) {
        def fileQueue = new ConcurrentLinkedQueue(submission)
        AtomicBoolean stillLooking = new AtomicBoolean(true)
        ThreadFactory threadFactory = Executors.defaultThreadFactory()
        def iFiles = fileQueue.iterator()
        final File pharmMlFile = null
        while (iFiles.hasNext() && stillLooking.get()) {
            final File file = iFiles.next()
            PharmMlDetector detective = new PharmMlDetector(file)
            Thread detectiveThread = threadFactory.newThread(detective)
            if (detectiveThread) {
                detectiveThread.setName("pharmML validation for ${file.name}")
                detectiveThread.start()
                detectiveThread.join()
                if (detective.isRecognisedFormat(file)) {
                    stillLooking.set(true)
                    pharmMlFile = file
                }
            } else {
                log.error "abstractPharmMlHandler.validate: Cannot start detection thread for file ${file.name}"
                return pharmMlFile
            }
        }

        if (!pharmMlFile && IS_INFO_ENABLED) {
            log.info "No PharmML to validate in ${submission.inspect()}"
        }
        return pharmMlFile
    }

    @Profiled(tag="abstractPharmMlHandler.fetchMainFilesFromRevision")
    static List<File> fetchMainFilesFromRevision(RevisionTransportCommand rev) {
        List<File> files = []
        List<File> locations = []
        rev?.files?.findAll{it.mainFile}.each{locations << it.path}
        locations.each { l ->
            File f = new File(l)
            if (f && f.exists() && f.canRead()) {
                files << f
            }
        }
        return files
    }

    @Profiled(tag="abstractPharmMlHandler.getResourceFromPharmML")
    static IPharmMLResource getResourceFromPharmML(final File f) {
        LibPharmMLImpl api = PharmMlFactory.getInstance().createLibPharmML()
        def stream = null
        IPharmMLResource resource = null
        try {
            stream = new BufferedInputStream(new FileInputStream(f))
            resource = api.createDomFromResource(stream)
        } catch(IOException x) {
            log.error(x.message, x)
        } finally {
            stream?.close()
            return resource
        }
    }

    static boolean savePharmML(File f, IPharmMLResource resource) {
        def api = PharmMlFactory.getInstance().createLibPharmML()
        def bos = new BufferedOutputStream(new FileOutputStream(f))
        boolean result = false
        try {
            api.save(bos, resource)
            result = true
        } catch(IOException e) {
            log.error "Cannot set metadataFile attribute for file $f.name"
        } finally {
            bos?.close()
            return result
        }
    }
}
