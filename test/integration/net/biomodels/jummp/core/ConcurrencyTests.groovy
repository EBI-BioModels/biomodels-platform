/**
* Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
* JGit, Apache Commons, Spring Framework, JUnit, MultithreadedTC 
* (or a modified version of that library), containing parts covered by the 
* terms of Common Public License, BSD license, Apache License v2.0, Eclipse 
* Distribution License v1.0, the licensors of this Program grant you additional 
* permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of JGit, Apache Commons, Spring Framework, 
* JUnit, MultithreadedTC used as well as that of the covered work.}
**/





package net.biomodels.jummp.core

import edu.umd.cs.mtc.MultithreadedTestCase
import edu.umd.cs.mtc.TestFramework
import java.util.concurrent.atomic.AtomicInteger
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.ModelFormat
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.plugins.git.GitManagerFactory
import net.biomodels.jummp.plugins.security.User
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.junit.*
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.core.io.FileSystemResourceLoader
import org.springframework.mock.web.MockServletContext
import static org.junit.Assert.*
import grails.util.Holders
import net.biomodels.jummp.core.concurrency.*

/*These tests are written to test the concurrent use of VcsManager.
They do not aim to test other concurrency aspects of Jummp, although
they may be extended to do so. The tests are based on the 
MultithreadedTestCase framework, and seek to test behaviour of the GitManager
when threads are accessing different models (which should occur concurrently)
and when threads are accessing the same model (which should result in sequential access)
*/

@Ignore
class ConcurrencyTests extends JummpIntegrationTest {

    def vcsService=Holders.applicationContext.getBean("vcsService")
    def grailsApplication=Holders.grailsApplication
    
    
    @Before
    void setUp() {
    	    createUserAndRoles()
    	    setUpVcs()
    }
    
    void setUpVcs() {
        GitManagerFactory gitService = new GitManagerFactory()
        gitService.grailsApplication = grailsApplication
        grailsApplication.config.jummp.plugins.git.enabled = true
        grailsApplication.config.jummp.vcs.workingDirectory = "target/vcs/git"
        grailsApplication.config.jummp.vcs.exchangeDirectory = "target/vcs/exchange"
        vcsService.vcsManager = gitService.getInstance()
        File exchange=new File("target/vcs/exchange")
        exchange.mkdirs()
        assertTrue(vcsService.isValid())
    }

    @After
    void tearDown() {
        FileUtils.deleteDirectory(new File("target/vcs/git"))
        FileUtils.deleteDirectory(new File("target/vcs/resource"))
        FileUtils.deleteDirectory(new File("target/vcs/repository"))
        FileUtils.deleteDirectory(new File("target/vcs/exchange"))
        vcsService.vcsManager = null
    }

   /*Tests that multiple threads can access different models concurrently*/
    @Test
    void testConcurrentAccess() {
        TestFramework.setGlobalRunLimit(120)
        authenticateAsAdmin()
        TestFramework.runOnce(new DontBlockWhenYouDontNeedTo())
    }
    
   /*Tests that multiple threads cant access the same model concurrently*/
    @Test
    void testConcurrentModelAccess() {
        TestFramework.setGlobalRunLimit(120)
        authenticateAsAdmin()
        TestFramework.runOnce(new BlockWhenYouShould())
    }

}
