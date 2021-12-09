usage() {
  echo "Please run batch-resubmission-init.sh to define required variables."
  echo "Then disble this instruction to allow the actual script to be run."
  exit 1
}
export -f usage
usage

ROOT=~/jummp-biomodels
source $ROOT/dev/batch-resubmission-init.sh
time $ROOT/grailsw run-script $ROOT/scripts/BatchSubmission.groovy --verbose >> $ROOT/logs/run-script-batch-submission-`date +%F`.log
