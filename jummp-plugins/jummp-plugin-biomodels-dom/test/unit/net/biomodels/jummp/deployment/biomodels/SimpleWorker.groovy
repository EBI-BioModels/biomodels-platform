package net.biomodels.jummp.deployment.biomodels

import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchCommand

import java.util.concurrent.CountDownLatch

/**
 * @author carankalle on 16/03/2019.
 */

class SimpleWorker implements Runnable {
    final ParameterSearchService service
    final CountDownLatch startLatch
    final CountDownLatch doneLatch
    final String query
    String result
    Throwable throwable

    SimpleWorker(CountDownLatch startLatch, CountDownLatch doneLatch, String q, ParameterSearchService service) {
        this.startLatch = Objects.requireNonNull startLatch
        this.doneLatch = Objects.requireNonNull doneLatch
        this.query = Objects.requireNonNull q
        this.service = Objects.requireNonNull service
    }

    @Override
    void run() {
        startLatch.await()

        def cmd = new ParameterSearchCommand(query: query, size: 100, is_curated: true)
        try {
            result = service.exportData(cmd)
        } catch (Throwable t) {
            throwable = t
        } finally {
            doneLatch.countDown()
        }
    }


}
