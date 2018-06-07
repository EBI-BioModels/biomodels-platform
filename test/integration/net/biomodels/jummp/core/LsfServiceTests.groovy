package net.biomodels.jummp.core

import net.biomodels.jummp.utils.NetworkUtils

import static org.junit.Assert.*
import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin
import net.biomodels.jummp.core.model.LSFApplication
import org.junit.Test

@TestMixin(IntegrationTestMixin)
class LsfServiceTests extends JummpIntegrationTest {

    /**
     * Dependency injection for LsfService
     */
    def lsfService

    @Test
    void testLFSService() {
        String jobId = lsfService.startLFSClusterJob(LSFApplication.MODEL_CLASSIFIER, 2000, 2, 1000)
        assertNotNull(jobId)
        String host = lsfService.getHostByJobId(jobId)
        assertNotNull(host)
        Integer port = lsfService.getPortByJobId(jobId)
        assertNotNull(port)

        // Wait to make sure that our job in cluster is ready
        Thread.sleep(60000)
        assertTrue(NetworkUtils.isReachable(host, port))
        lsfService.stopLSFClusterJob(jobId)
        assertFalse(NetworkUtils.isReachable(host, port))
    }
}
