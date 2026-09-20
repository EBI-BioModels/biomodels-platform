package net.biomodels.jummp.core

import static org.junit.Assert.*
import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin
import groovy.json.JsonBuilder
import net.biomodels.jummp.model.ModelFormat
import net.biomodels.jummp.model.ModellingApproach
import net.biomodels.jummp.plugins.configuration.VcsCommand
import net.biomodels.jummp.plugins.security.User
import org.apache.commons.io.FileUtils
import org.junit.*
import org.springframework.transaction.TransactionDefinition

/**
 * What the tests of the route of a submission (JBM-793) have in common, whether they call SubmissionService or
 * SubmissionController: a git repository and an exchange directory of their own, the users of the tests committed to
 * the database, and the files of a submission staged in a folder of the exchange directory.
 *
 * SubmissionService reads the exchange directory from the configuration once, when it starts, and in a test that is the
 * developer's real one. The tests point it at a temporary directory and put it back afterwards. The fixtures are copied,
 * never submitted from where they are, because a submission moves the files it is given.
 */
@TestMixin(IntegrationTestMixin)
abstract class SubmissionRouteTestBase extends JummpIntegrationTest {
    def grailsApplication
    def submissionService
    def modelService
    def fileSystemService

    /** A submission is stored in a transaction of its own, which only sees what has been committed. */
    protected static final Map txSettings = [propagationBehavior: TransactionDefinition.PROPAGATION_REQUIRES_NEW,
                                             isolationLevel     : TransactionDefinition.ISOLATION_READ_COMMITTED]

    protected File exchange
    private String originalExchangeDirectory

    @Before
    void setUp() {
        setupVcs()
        // committed in a transaction of its own, or the transaction a submission is stored in would not find the users
        User.withTransaction(txSettings) {
            createUserAndRoles()
            // a model gets the modelling approach OTHER if the submitter does not give one, and the tests have none
            new ModellingApproach(accession: "OTHER", name: "Other", resource: "test").save(flush: true, failOnError: true)
        }
        originalExchangeDirectory = submissionService.EXCH_DIR
        submissionService.EXCH_DIR = exchange.absolutePath
        assertNotNull(authenticateAsTestUser())
    }

    @After
    void tearDown() {
        submissionService.EXCH_DIR = originalExchangeDirectory
        FileUtils.deleteDirectory(new File("target/vcs/git"))
        FileUtils.deleteDirectory(new File("target/vcs/exchange"))
        modelService.vcsService.vcsManager = null
        // what was committed is not rolled back
        cleanupDatabase()
    }

    private void setupVcs() {
        FileUtils.deleteDirectory(new File("target/vcs/git"))
        FileUtils.deleteDirectory(new File("target/vcs/exchange"))
        exchange = VcsTestSupport.setupVcs(grailsApplication, modelService, fileSystemService, "target/vcs", "aaa")
    }

    /** Puts files in a new folder of the exchange directory, like an upload does, and returns the folder's name. */
    protected String stage(Map<String, String> filenamesAndText) {
        String folder = UUID.randomUUID() as String
        File dir = new File(exchange, folder)
        assertTrue dir.mkdirs()
        filenamesAndText.each { String name, String text -> new File(dir, name).text = text }
        folder
    }

    /** Copies a fixture into a new folder of the exchange directory, as it is uploaded. */
    protected String stageFile(File fixture, String name = fixture.name) {
        assertTrue("No such fixture: $fixture", fixture.exists())
        String folder = UUID.randomUUID() as String
        File dir = new File(exchange, folder)
        assertTrue dir.mkdirs()
        FileUtils.copyFile(fixture, new File(dir, name))
        folder
    }

    protected static Map<String, String> format(String identifier, String version = "*") {
        ModelFormat f = ModelFormat.findByIdentifierAndFormatVersion(identifier, version)
        assertNotNull("The format $identifier $version is not registered", f)
        [name: f.name, version: f.formatVersion]
    }

    protected static String metadata(Map fields) {
        new JsonBuilder(fields).toString()
    }

    /**
     * Runs a block with the FileSystemService using the exchange directory of the test. It gets the directory from
     * ConfigurationService, which reads the properties file, so it uses the developer's real one whatever the test has
     * put in the configuration.
     */
    protected def withExchangeDirectoryOfTheFileSystemService(Closure body) {
        def original = fileSystemService.configurationService
        fileSystemService.configurationService = [loadVcsConfiguration: { ->
            new VcsCommand(vcs: "git", workingDirectory: "target/vcs/git", exchangeDirectory: exchange.path)
        }]
        try {
            return body()
        } finally {
            fileSystemService.configurationService = original
        }
    }
}
