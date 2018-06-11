package net.biomodels.jummp.core

import net.biomodels.jummp.core.model.LSFClusterServer
import net.biomodels.jummp.utils.NetworkUtils
import org.junit.Assert

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
    void testLSFService() {
        String jobId = lsfService.startLFSClusterJob(LSFApplication.MODEL_CLASSIFIER, 2000, 2, 1000)
        assertNotNull(jobId)
        String host = lsfService.getHost(jobId)
        assertNotNull(host)
        Integer port = lsfService.getPort(jobId)
        assertNotNull(port)

        // Wait to make sure that our job in cluster is ready
        Thread.sleep(60000)
        assertTrue(NetworkUtils.isReachable(host, port))
        lsfService.stopLSFClusterJob(jobId)
        assertFalse(NetworkUtils.isReachable(host, port))
    }

    /**
     * Test always available service
     * This test will run during 2 days, check every 2s to see if the server reachable or not
     */
    @Test
    void testAlwaysAvailableLSFService() {
        LSFClusterServer server =
            lsfService.startAlwaysAvailableLSFClusterJob(LSFApplication.MODEL_CLASSIFIER, 2000, 2)

        Date dateEnd = new Date().plus(2)
        while (new Date().before(dateEnd)) {
            Assert.assertTrue(
                "$server.hostName:$server.port", NetworkUtils.isReachable(server.hostName, server.port))
            Thread.sleep(2000)
        }
    }
}
