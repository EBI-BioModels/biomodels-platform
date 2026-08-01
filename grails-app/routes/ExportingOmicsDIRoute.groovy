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

class ExportingOmicsDIRoute extends RouteBuilder {
    final String JAR_ARGS = '-jar ${body[jarPath]} ${body[jsonPath]} ${body[omicsdi]}'
    // exec:java has no timeout by default (ExecEndpoint.timeout defaults to Long.MAX_VALUE), so a
    // hung indexer jar would wedge this route's single seda consumer thread forever, silently
    // blocking every export request behind it. Fail loudly after 30 minutes instead.
    static final long EXEC_TIMEOUT_MS = 30 * 60 * 1000L

    @Override
    void configure() {
        from("seda:omicsDiExport")
        .setHeader("CamelExecCommandArgs", simple(JAR_ARGS))
        .to("exec:java?timeout=${EXEC_TIMEOUT_MS}")
        .process(new Processor() {
            @Override
            void process(Exchange exchange) throws Exception {
                println "The job has been launched!"
            }
        })
        // once the indexer jar (jummp.search.pathToIndexerExecutable, invoked above) has finished
        // writing the XML files, archive them to git. Runs on this same seda consumer thread, so the
        // caller that triggered the export (e.g. OmicsdiGitExportJob) is not blocked waiting for it.
        .to("bean:omicsdiService?method=archiveExportedXmlToGit")
    }
}
