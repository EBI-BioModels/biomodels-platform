/**
 * Copyright (C) 2010-2026 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

import net.biomodels.jummp.model.RepositoryFile
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.utils.RunScriptHelper
import org.apache.tika.detect.DefaultDetector
import org.apache.tika.metadata.Metadata
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * JBM-766: backfill for the *.xml.origin subset of the orphaned-files audit
 * (AuditOrphanedRevisionFiles.groovy).
 *
 * *.xml.origin is not a bug - scripts/BatchImportBiomodels.groovy deliberately writes it
 * (getOriginalFileForModel(), "submit first revision as *.origin") as part of the legacy
 * batch import, and it's meant to be a supported, visible file. It's genuinely committed to
 * those revisions' VCS trees; it was just never registered as a Revision.repoFiles entry, so
 * the Files/History tab never listed it and RepositoryFileService's cache-vs-tracking diff
 * (see JBM-766) flagged it as "orphaned".
 *
 * Unlike a real orphaned-file case, fixing this needs no VCS/git-history changes at all - the
 * content is already correct in the commit. This script only adds the missing RepositoryFile
 * database rows so repoFiles catches up with what's genuinely there. It scans every non-deleted
 * revision (no JBM-764 date cutoff - the batch import predates that bug entirely and could have
 * left this gap on any historical revision), not just the ones the audit script covered.
 *
 * Safe by default: runs as a dry run (prints what it would do, writes nothing) unless you pass
 * -Djummp.backfill.apply=true. Idempotent - already-tracked files are skipped, so it's safe to
 * re-run.
 *
 * Usage:
 *   ./grailsw run-script scripts/BackfillOriginFileTracking.groovy                       # dry run
 *   ./grailsw run-script scripts/BackfillOriginFileTracking.groovy -Djummp.backfill.apply=true   # writes
 *
 * @author Tung Nguyen <nvntung@gmail.com>
 */
class BackfillOriginFileTracking extends RunScriptHelper {
    def ctx
    private static final Logger logger = LoggerFactory.getLogger(BackfillOriginFileTracking.class)

    static final String ORIGIN_SUFFIX = ".xml.origin"

    @Override
    void run() {
        ctx.persistenceInterceptor?.init()
        boolean apply = System.getProperty("jummp.backfill.apply") == "true"
        int scanned = 0
        int alreadyTracked = 0
        int backfilled = 0
        List<String> errors = []
        try {
            println "Backfilling untracked ${ORIGIN_SUFFIX} files into Revision.repoFiles" +
                (apply ? "...\n" : " (DRY RUN - pass -Djummp.backfill.apply=true to write)...\n")

            String query = """\
SELECT r
FROM Revision AS r JOIN FETCH r.model AS m
WHERE r.deleted = false
  AND m.deleted = false
ORDER BY m.submissionId, r.revisionNumber
"""
            List<Revision> revisions = Revision.executeQuery(query, [:], [:])
            println "Scanning ${revisions.size()} non-deleted revisions (of non-deleted models)...\n"

            revisions.each { Revision revision ->
                scanned++
                String label = "${revision.model.submissionId} revision ${revision.revisionNumber}"
                try {
                    simpleRunAs(adminAuth, {
                        List<File> vcsFiles = ctx.vcsService.retrieveFiles(revision)
                        try {
                            if (!vcsFiles) {
                                return
                            }
                            List<File> originFiles = vcsFiles.findAll { it.name.endsWith(ORIGIN_SUFFIX) }
                            originFiles.each { File originFile ->
                                boolean tracked = revision.repoFiles.any { it.path == originFile.name }
                                if (tracked) {
                                    alreadyTracked++
                                    return
                                }
                                if (!apply) {
                                    println "Would backfill: ${label} -> ${originFile.name}"
                                    backfilled++
                                    return
                                }
                                String mimeType = detectMimeType(originFile)
                                RepositoryFile rf = new RepositoryFile(
                                    path: originFile.name,
                                    description: "Original file preserved from the historical BioModels import",
                                    mimeType: mimeType,
                                    hidden: false,
                                    mainFile: false,
                                    userSubmitted: false,
                                    revision: revision
                                )
                                revision.addToRepoFiles(rf)
                                if (revision.save(flush: true)) {
                                    println "Backfilled: ${label} -> ${originFile.name}"
                                    backfilled++
                                } else {
                                    String msg = "${label}: failed to save RepositoryFile for " +
                                        "${originFile.name} - ${rf.errors}"
                                    logger.error(msg)
                                    errors << msg
                                }
                            }
                        } finally {
                            // Clean up the throwaway exchange-directory checkout - all files
                            // from one retrieveFiles() call share the same UUID parent dir.
                            File exchangeDir = vcsFiles?.getAt(0)?.parentFile
                            if (exchangeDir?.exists()) {
                                exchangeDir.deleteDir()
                            }
                        }
                    })
                } catch (Exception e) {
                    String msg = "${label}: failed - ${e.message}"
                    logger.error(msg, e)
                    errors << msg
                }
                if (scanned % 200 == 0) {
                    println "...scanned ${scanned}/${revisions.size()}"
                }
            }
        } finally {
            ctx.persistenceInterceptor?.destroy()
        }

        println "\n=== Summary ==="
        println "Revisions scanned:         ${scanned}"
        println "Already tracked (skipped): ${alreadyTracked}"
        println "${apply ? 'Backfilled' : 'Would be backfilled'}:        ${backfilled}"
        if (errors) {
            println "Errors (${errors.size()}):"
            errors.each { println "  - ${it}" }
        }
        if (!apply && backfilled > 0) {
            println "\nThis was a dry run - nothing was written. Re-run with -Djummp.backfill.apply=true to apply."
        }
    }

    private static String detectMimeType(File file) {
        def sherlock = new DefaultDetector()
        String mimeType
        new BufferedInputStream(new FileInputStream(file)).withStream { is ->
            mimeType = sherlock.detect(is, new Metadata()).toString()
        }
        mimeType
    }
}

new BackfillOriginFileTracking(ctx: ctx).run()
