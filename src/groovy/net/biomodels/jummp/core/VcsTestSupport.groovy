package net.biomodels.jummp.core

import net.biomodels.jummp.plugins.git.GitManagerFactory

/**
 * What an integration test needs to store models in a version control system of its own, under target/, instead of
 * the one that the developer's configuration points at. It changes the configuration and the version control service
 * of the running application, so a test that calls it should clean up the directories in tearDown().
 */
class VcsTestSupport {
    /**
     * Sets up a git repository at {@code baseDir/git} with a model container of its own in it, and a directory for
     * exchanging files at {@code baseDir/exchange}.
     *
     * @param containerName the name of the directory in the repository that new models are created in
     * @return the exchange directory
     */
    static File setupVcs(def grailsApplication, def modelService, def fileSystemService, String baseDir,
            String containerName) {
        File root = new File(baseDir, "git").canonicalFile
        // the directory a new model's folder is created in has to exist
        File container = new File(root, containerName)
        File exchange = new File(baseDir, "exchange").canonicalFile
        [container, exchange].each { File directory ->
            assert directory.isDirectory() || directory.mkdirs(): "Cannot create $directory"
        }
        fileSystemService.root = root
        fileSystemService.currentModelContainer.set(container.absolutePath)
        GitManagerFactory gitService = new GitManagerFactory()
        gitService.grailsApplication = grailsApplication
        grailsApplication.config.jummp.plugins.git.enabled = true
        grailsApplication.config.jummp.vcs.workingDirectory = root.path
        grailsApplication.config.jummp.vcs.exchangeDirectory = exchange.path
        modelService.vcsService.vcsManager = gitService.getInstance()
        modelService.vcsService.modelContainerRoot = root.absolutePath
        modelService.vcsService.vcsManager.exchangeDirectory = exchange
        assert modelService.vcsService.isValid()
        exchange
    }
}
