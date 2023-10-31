#!/bin/bash

export KUBECONFIG=~/.kube/hlwpk.cfg

backup() {
  ns=$1
  kubectl get po -n $ns| grep jummp | xargs -n 1|grep jummp > tmp/pos.txt
 
  file=tmp/pos.txt
  pods=`cat $file`
  for po in $pods
  do
    dir="logs/$ns-$po"
    mkdir -p $dir
    kubectl logs $po -n $ns > $dir/$po.log 
    kubectl cp $ns/$po:/usr/local/tomcat/logs/ $dir/
  done
}

BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [[ $BRANCH == "k8sdev" ]]; then
  backup bmdev
else
  backup bmprod
fi


