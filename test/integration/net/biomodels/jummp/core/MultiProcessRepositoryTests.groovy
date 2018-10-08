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
* Apache Commons, JUnit (or a modified version of that library), containing parts
* covered by the terms of Common Public License, Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Apache Commons, JUnit used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.core


import net.biomodels.jummp.plugins.git.GitManager
import net.biomodels.jummp.utils.interprocess.InterJummpSync
import net.biomodels.jummp.utils.interprocess.UpdateAndTest
import org.apache.commons.io.FileUtils
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

import static org.junit.Assert.assertTrue

/*
* These tests ensure consistent multi-threaded and multi-process access to
* the GitManager implementation. A separate process is launched and three
* threads are created, all writing random numbers to the same file and
* updating the repository, subsequently verifying that the value for the
* file in the revision id returned by GitManager is correct.
*/
class MultiProcessRepositoryTests  {
    GitManager manager
    File exchange
    File modelFolder
    
    @Before
    void setUp() {
        // Setup exchange/model folders
        exchange=new File("target/vcs/exchange")
        modelFolder=new File("target/vcs/modelfolder")
        modelFolder.mkdirs()
        exchange.mkdirs()
        manager=new GitManager()
        manager.init(exchange)
    }

    @After
    void tearDown() {
       // Delete exchange/model folders
       FileUtils.deleteDirectory(new File("target/vcs/exchange"))
       FileUtils.deleteDirectory(new File("target/vcs/modelfolder"))
    }

    @Ignore @Test
    void testConcurrent() {
    	//start up sync server
        InterJummpSync sync=new InterJummpSync()
        sync.start(false, 50000)
        
        //launch concurrent tester grails script and wait for it to be ready
        String childProcess="grails test ConcurrentRepositoryTester"
        childProcess.execute()
        
        sync.waitForMessage("DoneStartup")
        
        //update the model
        manager.updateModel(modelFolder, [bigModel()], null)
        
        //launch three update and test class threads
        UpdateAndTest test1=new UpdateAndTest(manager, modelFolder, exchange, 1)
        Thread thread1=new Thread(test1)
        thread1.start()
        UpdateAndTest test2=new UpdateAndTest(manager, modelFolder, exchange, 2)
        Thread thread2=new Thread(test2)
        thread2.start()
        UpdateAndTest test3=new UpdateAndTest(manager, modelFolder, exchange, 3)
        Thread thread3=new Thread(test3)
        thread3.start()
        // wait for threads to terminate
        thread1.join()
        thread2.join()
        thread3.join()
        
        // ensure results for each thread are okay
        assertTrue(test1.dotestsPass())
        assertTrue(test2.dotestsPass())
        assertTrue(test3.dotestsPass())
        // wait for grails script to report its done, and test its result message
        log.error("Waiting for child process message")
        String errorMessage=sync.waitForMessageContaining("Error");
        assertTrue(errorMessage.contains("[]"))
        // terminate server
        sync.sendMessage("donetesting")
        Thread.sleep(200)
        sync.terminate()
    }
    
    private File bigModel() {
        return new File("test/files/BIOMD0000000272.xml")
    }
}
