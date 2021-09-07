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

    static RepositoryFileTransportCommand createRepoFile(final File file, final boolean isMainFile, final String
        description) {
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

    @CompileDynamic
    void processFolderOfSubmissions(File root, Pattern modelFolderPattern) {
        root.eachFileRecurse(FileType.DIRECTORIES) { File dir ->
            final String dirName = dir.name
            if (dirName ==~ modelFolderPattern) {
                doSubmissionDetected dir
            }
        }


        /*File modelFile = null
        // the main file is the SBML (.xml extension)
        // additionals should only contain <category_name>.zip -- the OMEX with the missing models
        List<File> additionals = []
        File[] model_dirs = models_location.listFiles()
        model_dirs = model_dirs.findAll {
            !it.name.startsWith(".")
        }
        for (File child : model_dirs) {
            println "Building the model files of the model ${child.name}"
//            if (child.name.endsWith('.xml')) {
//                modelFile = child
//            } else {
//                additionals << child
//            }
        }*/
    }
}
