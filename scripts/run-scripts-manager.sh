export JUMMP_CONFIG="$(eval echo ~$USER)/.jummp-biomodels-prod.properties"

function task1() {
    ./grailsw run-script scripts/FixModelPermission.groovy --verbose
}

function task2() {
    ./grailsw run-script scripts/RedisCacheManager.groovy --verbose
}

function task3() {
    ./grailsw run-script scripts/AuditOrphanedRevisionFiles.groovy --verbose
}

function task4() {
    # Dry run by default (prints what it would do, writes nothing). Add
    # -Djummp.backfill.apply=true to actually write the RepositoryFile rows.
    ./grailsw run-script scripts/BackfillOriginFileTracking.groovy --verbose
}

task3 # task1 task2 task4
