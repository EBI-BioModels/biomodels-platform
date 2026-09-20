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




import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.builder.RouteBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ExportingOmicsDIRoute extends RouteBuilder {
    // plain println only ever reaches the process's raw stdout (e.g. catalina.out), never any of
    // the named log4j appenders below - use a real logger so this shows up in jummp-debug.log.
    private static final Logger LOG = LoggerFactory.getLogger(ExportingOmicsDIRoute)

    // exec:java runs the indexer in its own JVM on the same host as Tomcat and MariaDB. Left
    // uncapped it defaults to 1/4 of the machine's RAM, which contributed to the OOM-killer
    // taking Tomcat down during the nightly export on 2026-09-19.
    static final String INDEXER_MAX_HEAP = '3g'
    final String JAR_ARGS = "-Xmx${INDEXER_MAX_HEAP} -jar " + '${body[jarPath]} ${body[jsonPath]} ${body[omicsdi]}'
    // exec:java has no timeout by default (ExecEndpoint.timeout defaults to Long.MAX_VALUE), so a
    // hung indexer jar would wedge this route's single seda consumer thread forever, silently
    // blocking every export request behind it. Fail loudly after 30 minutes instead.
    static final long EXEC_TIMEOUT_MS = 30 * 60 * 1000L

    // camel-exec is a runtime-only dependency, so these header names are hard-coded rather than
    // referenced via org.apache.camel.component.exec.ExecBinding (not on the compile classpath).
    static final String EXEC_EXIT_VALUE_HEADER = "CamelExecExitValue"
    static final String EXEC_STDERR_HEADER = "CamelExecStderr"
    // exec:java never fails the exchange on a non-zero exit and never logs the child process's
    // output, so an indexer that dies at startup (a bad datasource URL, a malformed
    // omicsdiSettings.json, ...) is completely silent - "The job has been launched!" used to fire
    // regardless and archiveExportedXmlToGit would then go on to commit whatever stale XML was
    // lying around. Surface the exit code + captured output, and only archive on a clean exit.
    static final int MAX_LOGGED_OUTPUT_CHARS = 8000
    static final String INDEXER_SUCCEEDED_PROPERTY = "omicsdiIndexerSucceeded"

    @Override
    void configure() {
        from("seda:omicsDiExport")
        .setHeader("CamelExecCommandArgs", simple(JAR_ARGS))
        .to("exec:java?timeout=${EXEC_TIMEOUT_MS}")
        .process(new Processor() {
            @Override
            void process(Exchange exchange) throws Exception {
                Integer exitValue = exchange.in.getHeader(EXEC_EXIT_VALUE_HEADER, Integer)
                String stderr = readAsString(exchange, exchange.in.getHeader(EXEC_STDERR_HEADER))
                String stdout = readAsString(exchange, exchange.in.getBody())

                if (exitValue == null || exitValue == 0) {
                    LOG.info("The job has been launched! The OmicsDI indexer exited cleanly (exit code {}).", exitValue)
                    if (stderr?.trim()) {
                        LOG.warn("OmicsDI indexer wrote to stderr despite exiting cleanly:\n{}", truncate(stderr))
                    }
                    exchange.setProperty(INDEXER_SUCCEEDED_PROPERTY, Boolean.TRUE)
                } else {
                    LOG.error("The OmicsDI indexer failed (exit code {}); skipping the git archive step so a " +
                        "failed run cannot publish stale XML.\n--- indexer stderr ---\n{}\n--- indexer stdout ---\n{}",
                        exitValue, truncate(stderr), truncate(stdout))
                    exchange.setProperty(INDEXER_SUCCEEDED_PROPERTY, Boolean.FALSE)
                }
            }
        })
        // once the indexer jar (jummp.search.pathToIndexerExecutable, invoked above) has finished
        // writing the XML files, archive them to git - but only if it actually succeeded. Runs on
        // this same seda consumer thread, so the caller that triggered the export (e.g.
        // OmicsdiGitExportJob) is not blocked waiting for it.
        .filter(property(INDEXER_SUCCEEDED_PROPERTY).isEqualTo(Boolean.TRUE))
            .to("bean:omicsdiService?method=archiveExportedXmlToGit")
        .end()
    }

    /**
     * Best-effort conversion of an exec result stream/body to a String for logging. The exec
     * component hands us stderr as a {@code ByteArrayInputStream} header and stdout via the body;
     * a conversion failure here must never mask the underlying indexer failure.
     */
    private static String readAsString(Exchange exchange, Object value) {
        if (value == null) {
            return null
        }
        try {
            return exchange.context.typeConverter.convertTo(String, exchange, value)
        } catch (Exception e) {
            return "<unreadable: ${e.message}>"
        }
    }

    private static String truncate(String text) {
        if (!text?.trim()) {
            return "(none)"
        }
        if (text.length() <= MAX_LOGGED_OUTPUT_CHARS) {
            return text
        }
        text.substring(0, MAX_LOGGED_OUTPUT_CHARS) + "\n... [truncated ${text.length() - MAX_LOGGED_OUTPUT_CHARS} chars]"
    }
}
