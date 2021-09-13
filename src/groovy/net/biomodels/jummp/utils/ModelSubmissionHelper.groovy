package net.biomodels.jummp.utils

import groovy.io.FileType
import groovy.transform.CompileDynamic
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand as RepoFTC
import org.apache.camel.CamelContext
import org.apache.camel.component.seda.SedaConsumer
import org.codehaus.groovy.grails.support.PersistenceContextInterceptor as PCI
import org.hibernate.SessionFactory
import org.springframework.context.ApplicationContext
import org.springframework.orm.hibernate4.SessionHolder
import org.springframework.transaction.support.TransactionSynchronizationManager as TxnSyncMnger
import grails.plugin.springsecurity.SpringSecurityUtils

import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.model.ModelFormat
import net.biomodels.jummp.model.ModellingApproach
import net.biomodels.jummp.model.RepositoryFile
import net.biomodels.jummp.model.Revision

import java.util.regex.Pattern

class ModelSubmissionHelper {
    static ApplicationContext ctx
    private CamelContext camelContext

    RepoFTC createRepoFile(final File file, final boolean isMainFile,
                                  final String description) {
        new RepoFTC(path: file.absolutePath, mainFile: isMainFile,
            userSubmitted: true, hidden: false, description: description)
    }

    static SessionHolder getCurrentSession() {
        SessionFactory factory = ctx.getBean "sessionFactory", SessionFactory
        TxnSyncMnger.getResource(factory) as SessionHolder
    }

    /**
     * Convenience method for performing operations within a Hibernate Session.
     * @param callback a closure defining the work to be done
     * @param submissionFolder the model folder on which to operate
     * @return the result of the work performed, possibly null.
     */
    Object doInSession(Closure callback) {
        // set up a Hibernate session
        PCI persistenceInterceptor = ctx.getBean "persistenceInterceptor", PCI
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

    static File createSimpleMatlabModel(final String filename, final File location) {
        String content = """
%% String Manipulations with Arrays
% *back to* <https://fanwangecon.github.io *Fan*>*'s* <https://fanwangecon.github.io/Math4Econ/
% *Intro Math for Econ*>*,*  <https://fanwangecon.github.io/M4Econ/ *Matlab Examples*>*,
% or* <https://fanwangecon.github.io/CodeDynaAsset/ *Dynamic Asset*> *Repositories*
%% String Array
% Three title lines, with double quotes:

ar_st_titles = ["Title1","Title2","Title3"]';
disp(ar_st_titles);
%%
% Three words, joined together, now single quotes, this creates one string,
% rather than a string array:

st_titles = ['Title1','Title2','Title3'];
disp(st_titles);
%% String Cell Array
% Create a string array:

ar_st_title_one = {'Title One Line'};
ar_st_titles = {'Title1','Title2','Title3'};
disp(ar_st_title_one);
disp(ar_st_titles);
%%
% Add to a string array:

ar_st_titles{4} = 'Title4';
disp(ar_st_titles);
"""
        File tempFile = File.createTempFile(filename, ".m", location)
        tempFile.write(content)
        return tempFile
    }

}
