package net.biomodels.jummp.plugins.omicsdi

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Once a day, if any Model/Revision activity happened since the last run (see
 * {@link OmicsdiService#markExportDirty}), asynchronously (re-)generates the OmicsDI XML export
 * and archives it to the EBI-BioModels/repository-archives git repo.
 *
 * This batches per-update triggers into a single daily export + commit + push, rather than
 * running one per Model/Revision change. The actual export (indexer jar via Camel's exec:java) and
 * the git archive step that follows it both run asynchronously off the seda:omicsDiExport queue
 * (see {@code ExportingOmicsDIRoute} under grails-app/routes) - this job only has to enqueue the
 * request and returns immediately; it does not wait for the export to finish, so the dirty flag is
 * cleared later, by {@link OmicsdiService#archiveExportedXmlToGit}, once the pipeline actually
 * completes.
 */
class OmicsdiGitExportJob {
    static final Logger LOGGER = LoggerFactory.getLogger(OmicsdiGitExportJob.class)

    def omicsdiService

    static triggers = {
        // Every day at 22:30
        cron name: "omicsdiGitExport", cronExpression: "0 30 22 * * ?"
    }

    def execute() {
        if (!omicsdiService.isExportDirty()) {
            LOGGER.info("No Model/Revision activity since the last OmicsDI export; skipping.")
            return
        }
        LOGGER.info("Model/Revision activity detected; enqueuing an OmicsDI export + git archive...")
        omicsdiService.exportOmicsdiEntries()
    }
}
