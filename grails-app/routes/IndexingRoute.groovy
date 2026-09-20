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
import org.apache.camel.ShutdownRunningTask
import org.apache.camel.builder.RouteBuilder

class IndexingRoute extends RouteBuilder {
    // Every message launches its own indexer JVM and up to 15 can run at once (concurrentConsumers
    // below). Left uncapped each one defaults to 1/4 of the machine's RAM, which contributed to the
    // OOM-killer taking Tomcat down on 2026-09-19 (JBM-797). One model's indexData.json is far
    // smaller than a full OmicsDI export, so this is well below ExportingOmicsDIRoute's cap.
    static final String INDEXER_MAX_HEAP = '1g'
    final String JAR_ARGS = "-Xmx${INDEXER_MAX_HEAP} -jar " + '${body[jarPath]} ${body[jsonPath]}'

    @Override
    void configure() {
        from("seda:exec?concurrentConsumers=15")
        .shutdownRunningTask(ShutdownRunningTask.CompleteAllTasks)
        .setHeader("CamelExecCommandArgs", simple(JAR_ARGS))
        .to("exec:java")
        .process(new Processor() {
            void process(Exchange exchange) {
                def msg = exchange.in
                def headers = msg.headers
                String content = msg.getBody(String.class)
                println "${Thread.currentThread().name} -- Indexing of $headers produced $content"
            }
        })
    }
}
