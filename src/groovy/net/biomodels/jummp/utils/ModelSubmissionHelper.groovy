package net.biomodels.jummp.utils

import net.biomodels.jummp.core.model.RepositoryFileTransportCommand
import org.apache.camel.CamelContext
import org.apache.camel.component.seda.SedaConsumer
import org.codehaus.groovy.grails.support.PersistenceContextInterceptor
import org.hibernate.SessionFactory
import org.springframework.context.ApplicationContext
import org.springframework.orm.hibernate4.SessionHolder
import org.springframework.transaction.support.TransactionSynchronizationManager

class ModelSubmissionHelper {
    static ApplicationContext ctx
    private CamelContext camelContext

    static RepositoryFileTransportCommand createRepoFile(final File file, final boolean isMainFile,
                                                         final String description) {
        new RepositoryFileTransportCommand(path: file.absolutePath, mainFile: isMainFile,
            userSubmitted: true, hidden: false, description: description)
    }

    static SessionHolder getCurrentSession() {
        SessionFactory factory = ctx.getBean "sessionFactory", SessionFactory
        TransactionSynchronizationManager.getResource(factory) as SessionHolder
    }

    /**
     * Convenience method for performing operations within a Hibernate Session.
     * @param callback a closure defining the work to be done
     * @param submissionFolder the model folder on which to operate
     * @return the result of the work performed, possibly null.
     */
    Object doInSession(Closure callback) {
        // set up a Hibernate session
        PersistenceContextInterceptor persistenceInterceptor = ctx.getBean "persistenceInterceptor", PersistenceContextInterceptor
        SessionHolder session = null
        try {
            persistenceInterceptor?.init()
            session = getCurrentSession()
            callback.call(session)
        } finally {
            if (!session?.rollbackOnly) {
                persistenceInterceptor?.flush()
            }
            persistenceInterceptor?.destroy()
        }
    }

    void awaitCompletionOfIndexingJobs() {
        // wait for pending indexing jobs to complete before stopping
        def indexRequestDispatcher = camelContext.routes.find {
            // we use seda:exec to invoke the indexer
            it.consumer.endpoint.endpointKey.startsWith("seda://exec")
        }.consumer as SedaConsumer

        int pendingIndexingJobs = indexRequestDispatcher.pendingExchangesSize
        while (pendingIndexingJobs > 0) {
            println("Waiting for ${pendingIndexingJobs} models to be indexed...")
            Thread.sleep(30000)
            pendingIndexingJobs = indexRequestDispatcher.pendingExchangesSize
        }
    }
}
