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
import net.biomodels.jummp.plugins.configuration.VcsCommand
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

    /**
     * Stand-in for the real ConfigurationService, providing just enough of
     * loadVcsConfiguration() for FileSystemService.findRoot() (called from
     * afterPropertiesSet(), which Spring invokes as soon as the service bean is created -
     * i.e. before setUp() gets a chance to configure anything on the service instance itself).
     */
    private static class StubConfigurationService {
        VcsCommand vcsCommand
        VcsCommand loadVcsConfiguration() { vcsCommand }
    }

    @Before
    void setUp() {
        // FileSystemService no longer has a grailsApplication property at all - it now reads its
        // VCS config via configurationService.loadVcsConfiguration() (see findRoot()). None of the
        // tests below exercise that path themselves (they drive root/currentModelContainer/
        // maxContainerSize directly and call findCurrentModelContainer(), which never touches
        // configurationService) - but FileSystemService implements InitializingBean, so
        // afterPropertiesSet() (and so findRoot()/configurationService) runs unconditionally the
        // moment `service` is first accessed below, regardless of what any test method needs.
        // configurationService has to be registered as a real Spring bean (defineBeans, before
        // that first access) rather than just assigned onto `service` afterwards - by then
        // afterPropertiesSet() has already run and failed.
        final String workingDirectoryPath = "target/vcs/workingDirectory/"
        parentLocation = new File(workingDirectoryPath)
        FileUtils.deleteDirectory(parentLocation)
        // findRoot() logs (but tolerates) a missing root, but afterPropertiesSet()'s own
        // getFolders(root) call NPEs on a root that doesn't exist yet (File#listFiles() returns
        // null, not an empty array, for a non-existent directory).
        parentLocation.mkdirs()
        defineBeans {
            configurationService(StubConfigurationService) {
                vcsCommand = new VcsCommand(vcs: "git", workingDirectory: workingDirectoryPath)
            }
        }
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

    @Test
    void aNewContainerIsCreatedIfTheCurrentOneIsFull() {
        def parent = service.findCurrentModelContainer()
        for (int i = 0; i < service.maxContainerSize + 1; ++i) {
            String newModel = UUID.randomUUID().toString()
            File m = new File(parent, newModel)
            assertTrue m.mkdirs()
        }

        def container = service.findCurrentModelContainer()
        assertTrue "$container ends with ttu", container.endsWith('ttu')
        assertEquals "$container is empty", 0, new File(container).list().length
    }
}

