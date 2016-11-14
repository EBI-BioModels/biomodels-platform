/**
 * Copyright (C) 2010-2015 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
 * Apache Commons, JUnit (or a modified version of that library), containing parts
 * covered by the terms of Common Public License, Apache License v2.0, the licensors of this
 * Program grant you additional permission to convey the resulting work.
 * {Corresponding Source for a non-source form of such a combination shall
 * include the source code for the parts of Apache Commons, JUnit used as well as
 * that of the covered work.}
 **/

package net.biomodels.jummp.core

import grails.test.mixin.TestFor
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.apache.commons.io.FileUtils
import org.junit.*
import static org.junit.Assert.*

@TestFor(FileSystemService)
class FileSystemServiceTests {
    File parentLocation

    @Before
    void setUp() {
        final String workingDirectoryPath = "target/vcs/workingDirectory/"
        grailsApplication.config.jummp.vcs.plugin = "git"
        grailsApplication.config.jummp.vcs.workingDirectory = workingDirectoryPath
        service.grailsApplication = grailsApplication
        parentLocation = new File(workingDirectoryPath)
        FileUtils.deleteDirectory(parentLocation)
        service.root = parentLocation
        def container = new File(parentLocation, "ttt")
        container.mkdirs()
        service.currentModelContainer.set(container.absolutePath)
        service.maxContainerSize = 10
    }

    @After
    void tearDown() {
        FileUtils.deleteQuietly(parentLocation)
    }

    @Test
    void resetEventsInContainerNamesAreHandledGracefully() {
        assertEquals 10, service.maxContainerSize
        assertTrue(service.findCurrentModelContainer().endsWith("ttt"))
        mockModelFolders(9)
        assertTrue(service.findCurrentModelContainer().endsWith("ttt"))
        mockModelFolders(1)
        assertTrue(service.findCurrentModelContainer().endsWith("ttu"))
        mockModelFolders(9)
        assertTrue(service.findCurrentModelContainer().endsWith("ttu"))
        mockModelFolders(1)
        assertTrue(service.findCurrentModelContainer().endsWith("ttv"))
        File newRoot = new File(parentLocation, "abz")
        service.currentModelContainer.set(newRoot.absolutePath)
        service.maxContainerSize = 1
        assertTrue(service.findCurrentModelContainer().endsWith("abz"))
        mockModelFolders(1)
        assertTrue(service.findCurrentModelContainer().endsWith("aca"))
        mockModelFolders(1)
        assertTrue(service.findCurrentModelContainer().endsWith("acb"))
        newRoot = new File(parentLocation, "zzz")
        service.currentModelContainer.set(newRoot.absolutePath)
        mockModelFolders(1)
        assertTrue(service.findCurrentModelContainer().endsWith("aaaa"))
    }

    @Test
    void concurrentInsertionsAreHandledGracefully() {
        final int CONTAINER_SIZE = 2
        service.maxContainerSize = CONTAINER_SIZE
        int poolSize = CONTAINER_SIZE + 1
        def pool = Executors.newFixedThreadPool(poolSize)
        def latch = new CountDownLatch(1)
        for (int i = 0; i < poolSize; ++i) {
            pool.submit(new Runnable() {
                void run() {
                    latch.await()
                    mockModelFolders(CONTAINER_SIZE)
                }
            })
        }
        latch.countDown()
        pool.shutdown()
        pool.awaitTermination(1, TimeUnit.SECONDS)
        def cancelled = pool.shutdownNow()
        assertEquals 0, cancelled.size()
        assertTrue pool.isTerminated()

        // ttt, ttu and ttv are full
        parentLocation.listFiles().each { d ->
            assertTrue d.isDirectory()
            assertEquals CONTAINER_SIZE, d.list().length
        }

        // we now have an empty new container
        assertTrue service.findCurrentModelContainer().endsWith('ttw')
    }

    private void mockModelFolders(final int count) {
        count.times {
            synchronized(this) {
                def parent = service.findCurrentModelContainer()
                String newModel = UUID.randomUUID().toString()
                File m = new File(parent, newModel)
                assertTrue m.mkdirs()
            }
        }
    }
}

