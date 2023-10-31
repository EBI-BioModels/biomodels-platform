#!/bin/bash

export KUBECONFIG=~/.kube/hlwpk.cfg

backup() {
  ns=$1
  kubectl get po -n $ns| grep jummp | xargs -n 1|grep jummp > tmp/pos.txt
 
  file=tmp/pos.txt
  pods_str=$(cat $file | tr "\n" " ")
  pods=($pods_str)
  for po in "${pods}"
  do
    dir="logs/$ns-$po"
    mkdir $dir
    kubectl logs $po -n $ns > $dir/$pos.log 
    kubectl cp $ns/$pos:/usr/local/tomcat/logs/ $dir/
  done
}

BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [[ $BRANCH == "k8sdev" ]]; then
  backup bmdev
else
  backup bmprod
fi


