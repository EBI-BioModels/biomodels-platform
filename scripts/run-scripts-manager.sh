export JUMMP_CONFIG="$(eval echo ~$USER)/.jummp-biomodels-prod.properties"

function task1() {
    ./grailsw run-script scripts/FixModelPermission.groovy --verbose
}

function task2() {
    ./grailsw run-script scripts/RedisCacheManager.groovy --verbose
}

task2 # task1
