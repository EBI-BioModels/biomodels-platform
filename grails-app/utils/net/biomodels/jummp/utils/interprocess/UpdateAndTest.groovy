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
**/




/* author: Raza Ali: raza.ali@ebi.ac.uk */

package net.biomodels.jummp.utils.interprocess

/* Convenience class to allow the multi-process test components to have the same
** model update and verification mechanism. */ 

class UpdateAndTest implements Runnable {
        File myDir
        File modelFolder
        def manager
        boolean allTestsPass=false
        List<String> errorMessages=new LinkedList<String>()
        
        public void setModel(File model) {
        	modelFolder=model
        }
        
        public void init(File exchange, int counter) {
        	Random random=new Random()
        	counter+=random.nextInt(100)
        	myDir=new File(exchange, ""+counter)
        	myDir.mkdirs()
        }
        
        public void setManager(def m) {
        	manager=m
        }
        
        
        public UpdateAndTest() {
        }
        
        public UpdateAndTest(def m, File model, File exchange, int counter) {
            init(exchange, counter)
            setModel(model)
            setManager(m)
        }
        
        boolean dotestsPass() {
        	System.out.println("TEST PASSED: "+allTestsPass)
        	return allTestsPass
        }
        
        void run() {
            // loops a fixed number of times, updating a file with a random number
            // and saving it in the model, then retrieving the model and verifying
            // the file is correct. Sleeps after each iteration to allow competing
            // processes access to the repository lock.
            File importFile=new File(myDir, "importTest.txt")
            boolean testResult=true
            for (int i=0; i<200; i++) {
                String testText=""+(Math.random()*System.currentTimeMillis())
                importFile.setText(testText)
                String rev=manager.updateModel(modelFolder, [importFile], null, "Update by ${myDir.getName()} iteration ${i}")
                List<File> files=manager.retrieveModel(modelFolder, rev);
                testResult = testResult & testFileCorrectness(files, "importTest.txt", testText)
                Thread.sleep(80)
            }
            allTestsPass=testResult
        }
        
        List getAllErrors() {
        	return errorMessages;
        }

        boolean testFileCorrectness(List<File> files, String filename, String filetext)
        {
        	try
        	{
        	boolean testResult=true
        	int fileIndex=-1
        	files.eachWithIndex { file, i -> 
        		if (file.name == filename) fileIndex=i
        	};
        	testResult= testResult & fileIndex>-1
        	if (!testResult) {
        		errorMessages.add("File: "+filename+" was not found, but was expected")
        		return testResult
        	}
        	List<String> lines = files.get(fileIndex).readLines()
        	testResult = testResult & lines.size()==1
        	if (!testResult) {
        		errorMessages.add("File: "+filename+" had a different number of lines than expected")
        		return testResult
        	}
        	testResult = testResult & filetext==lines[0]
        	if (!testResult) {
        		errorMessages.add("File: "+filename+" had a different text, expected "+filetext+" got "+lines[0])
        	}
        	return testResult
        	}
        	catch(Exception e) {
        		e.printStackTrace()
        		errorMessages.add("EXCEPTION: "+e.toString())
        		return false;
        	}
        }
}
    

