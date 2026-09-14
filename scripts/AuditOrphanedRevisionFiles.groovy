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

import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.utils.RunScriptHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.time.Instant

/**
 * JBM-766: read-only audit for the JBM-764 orphaned-additional-files bug.
 *
 * JBM-764 (fixed in commit e7e7ed47c) stopped *new* submissions from silently dropping an
 * additional file from Revision.repoFiles tracking while leaving it physically committed to
 * the model's VCS working tree. It cannot retroactively fix revisions whose commit was already
 * created before the fix landed - those commits genuinely contain files that Revision.repoFiles
 * (and therefore the Files/History tab) never lists.
 *
 * This script finds them: for every non-deleted revision of a non-deleted model, uploaded
 * before the fix's cutoff, it checks out the revision's actual commit via VcsService and
 * compares the real file set against Revision.repoFiles. Any file present in the commit but
 * absent from repoFiles is reported as orphaned.
 *
 * Deliberately read-only:
 *  - never calls RepositoryFileService.retrieveFiles()/get()/updateModelRevisionCache(), so it
 *    never populates or mutates the on-disk model cache as a side effect;
 *  - never writes to the database or to VCS;
 *  - deletes the throwaway exchange-directory checkout VcsService.retrieveFiles() hands back
 *    for each revision once that revision has been compared (per its own Javadoc: "It's the
 *    responsibility of the caller to delete the file when it is not needed any more"), so
 *    running this over the whole instance doesn't leave temp checkouts behind.
 *
 * Usage:
 *   ./grailsw run-script scripts/AuditOrphanedRevisionFiles.groovy
 *
 * Optional override (rarely needed - only if re-running after a later fix/backfill and you
 * want a different cutoff):
 *   ./grailsw run-script scripts/AuditOrphanedRevisionFiles.groovy -Djummp.audit.cutoff=2026-09-14T08:42:54Z
 *
 * @author Tung Nguyen <nvntung@gmail.com>
 */
class AuditOrphanedRevisionFiles extends RunScriptHelper {
    def ctx
    private static final Logger logger = LoggerFactory.getLogger(AuditOrphanedRevisionFiles.class)

    // Authored instant of JBM-764's fix commit (e7e7ed47c). Any revision uploaded at or after
    // this instant went through the fixed submission code path and shouldn't need auditing.
    static final String DEFAULT_CUTOFF = "2026-09-14T08:42:54Z"

    // Housekeeping file the app itself filters out of cache/VCS listings (see
    // RepositoryFileService.hideSomeFileTypes) - never a genuine orphan.
    static final String IGNORED_FILENAME = "indexData.json"

    @Override
    void run() {
        ctx.persistenceInterceptor?.init()
        int scanned = 0
        int affected = 0
        int orphanedFilesTotal = 0
        List<String> errors = []
        try {
            Date cutoff = parseCutoff()
            println "Auditing revisions uploaded before ${cutoff} (UTC) for files present in " +
                "VCS but not tracked in Revision.repoFiles...\n"

            String query = """\
SELECT r
FROM Revision AS r JOIN FETCH r.model AS m
WHERE r.deleted = false
  AND m.deleted = false
  AND r.uploadDate < :cutoff
ORDER BY m.submissionId, r.revisionNumber
"""
            List<Revision> revisions = Revision.executeQuery(query, [cutoff: cutoff])
            println "Found ${revisions.size()} non-deleted revisions (of non-deleted models) uploaded before the cutoff.\n"

            revisions.each { Revision revision ->
                scanned++
                String modelId = revision.model.submissionId
                String label = "${modelId} revision ${revision.revisionNumber} (vcsId ${revision.vcsId}, uploaded ${revision.uploadDate})"
                try {
                    simpleRunAs(adminAuth, {
                        List<File> vcsFiles = ctx.vcsService.retrieveFiles(revision)
                        try {
                            if (!vcsFiles) {
                                logger.warn("${label}: VcsService returned no files - skipping.")
                                return
                            }
                            Set<String> trackedNames = revision.repoFiles*.path as Set
                            List<String> orphaned = vcsFiles*.name.findAll { String name ->
                                name != IGNORED_FILENAME && !trackedNames.contains(name)
                            }
                            if (orphaned) {
                                affected++
                                orphanedFilesTotal += orphaned.size()
                                println "AFFECTED: ${label}"
                                println "  tracked (repoFiles): ${trackedNames.sort()}"
                                println "  orphaned (in VCS, not tracked): ${orphaned.sort()}"
                                println ""
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
                    String msg = "${label}: failed to audit - ${e.message}"
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
        println "Revisions scanned:  ${scanned}"
        println "Revisions affected: ${affected}"
        println "Orphaned files:     ${orphanedFilesTotal}"
        if (errors) {
            println "Errors (${errors.size()}):"
            errors.each { println "  - ${it}" }
        }
    }

    private static Date parseCutoff() {
        String raw = System.getProperty("jummp.audit.cutoff") ?: DEFAULT_CUTOFF
        Date.from(Instant.parse(raw))
    }
}

new AuditOrphanedRevisionFiles(ctx: ctx).run()
