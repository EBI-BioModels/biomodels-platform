/**
 * Copyright (C) 2010-2020 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
 */


import groovyx.gpars.GParsPool
import net.biomodels.jummp.core.model.CurationState
import net.biomodels.jummp.core.model.ModelFormatTransportCommand as MFTC
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand as RFTC
import net.biomodels.jummp.core.model.RevisionTransportCommand as RTC
import net.biomodels.jummp.core.model.ValidationState
import net.biomodels.jummp.model.ModelFormat
import net.biomodels.jummp.utils.ModelSubmissionHelper
import net.biomodels.jummp.utils.RunScriptHelper
import net.biomodels.jummp.utils.redis.RedisService
import org.apache.camel.CamelContext
import org.springframework.security.core.Authentication

import java.time.Duration
import java.time.Instant

/**
 * Submits multiple models concurrently to test model identifier generators
 * in the context of multiple instance deployment
 *
 * @author <a href="mailto:nvntung@gmail.com">Tung Nguyen</a> on 04/06/20.
 */
class ConcurrentModelSubmitter {
    def ctx

    CamelContext camelContext
    ModelSubmissionHelper helper
    static final String adminUsername = System.getenv("ADMIN_USER")
    // auth token for admin account; used by worker threads to publish models
    static final Authentication adminAuth = RunScriptHelper.createTokenForUser(adminUsername)
    RedisService redisService

    void init() {
        redisService = ctx.getBean('redisService', RedisService)
        camelContext = ctx.getBean('camelContext', CamelContext)
        helper = new ModelSubmissionHelper(ctx: ctx, camelContext: camelContext)
    }

    void runBatchSubmission() {
        println "Started the job: ${new Date().format("dd/MM/yyyy HH:mm:ss")}"

        init()
        String duration
        Instant startTime = Instant.now()
        ctx.persistenceInterceptor?.init()
        try {
            Map revisions = buildDataSubmission()
            final int POOL_SIZE = 8
            GParsPool.withPool(POOL_SIZE) {
                revisions.eachParallel { RTC cmd, List files ->
                    RunScriptHelper.simpleRunAs(adminAuth, {
                        ctx.persistenceInterceptor?.init()
                        try {
                            submitModel(files, cmd)
                        } catch (Exception e) {
                            String message = """Cannot submit the models due to ${e.message}"""
                            println(message)
                            e.printStackTrace()
                        } finally {
                            ctx.persistenceInterceptor?.destroy()
                        }
                    })
                }
            }
        } finally {
            ctx.persistenceInterceptor?.destroy()
            duration = Duration.between(startTime, Instant.now())
            String formattedDuration = duration.toString()
            println "Submission lasting in $formattedDuration"
        }
        println "Completed the job: ${new Date().format("dd/MM/yyyy HH:mm:ss")}"
    }

    Map buildDataSubmission() {
        Map data = [:]
        String tmpDir = System.getProperty("java.io.tmpdir")
        File location = new File(tmpDir)
        for (int i = 1; i <= 10; i++) {
            File mainFile = createSimpleMatlabModel("MODEL$i", location)
            String desc = "This is a sample Matlab model $i"
            RFTC mainRFTC = ModelSubmissionHelper.createRepoFile(mainFile, true, desc)
            ModelFormat fmt = ModelFormat.findByIdentifierAndName("matlab", "MATLAB (Octave)")
            MFTC fmtCmd = new MFTC(identifier: "matlab", name: "MATLAB (Octave)", formatVersion: fmt.formatVersion)
            boolean isValid = true
            String modelName = "This is a sample Matlab model $i"
            String modelDesc = "This model simulates how to submit multiple models to BioModels concurrently"
            ModelTransportCommand modelCmd = new ModelTransportCommand(deleted: false)
            RTC command = new RTC(files: [mainRFTC], format: fmtCmd, context: ctx,
                validated: isValid, name: modelName, description: modelDesc,
                validationLevel: ValidationState.APPROVED,
                curationState: CurationState.NON_CURATED, minorRevision: false,
                comment: "Push the first commit of '$modelName'.", model: modelCmd)
            data.put(command, [mainRFTC] as List)
        }
        return data
    }

    void submitModel(final List files, final RTC revision) {
        println "Submitting the model revision ${revision.dump()} with the repository files ${files.dump()} with the " +
            "submission id: ${revision.model.submissionId}"
        ctx.modelService.uploadValidatedModel(files, revision)
        helper.awaitCompletionOfIndexingJobs()
        /*String submissionId = ""
        synchronized (this) {
            submissionId = ctx.modelService.submissionIdGenerator.generate()
        }
        println "The new model identifier is $submissionId"*/
    }

    private File createSimpleMatlabModel(final String filename, final File location) {
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

    void startBatchSubmissionWithTrigger() {
        Boolean running = false
        while (!running) {
            // sleep for 1 second
            println("Waiting 1 second...")
            Thread.sleep(1000)
            running = redisService.doRedisGet("run-batch-submission").toBoolean()
        }
        // ready to go
        runBatchSubmission()
    }
}
/**
 * Steps to enable the trigger
 * 1. Run this script on different terminals to simulate BioModels' multiple concurrent running instances
 * ./grailsw run-script scripts/SubmitModelsConcurrently.groovy >> logs/run-script-`date +%F`.log
 *
 * 2. When the lines 'Waiting 1 second...' begin appearing in the log file, it's time to make a bang by running the
 * following from Redis CLI
 * SET run-batch-submission true
 *
 * 3. Take your coffee and watch the log file
 */
new ConcurrentModelSubmitter(ctx: ctx).startBatchSubmissionWithTrigger()
