#!/bin/bash
#
# Copyright (c) Orient Technologies LTD (http://www.orientechnologies.com)
#
export ORIENTDB_HOST=`ip -4 -o addr show dev ethwe 2> /dev/null | awk '{split($4,a,"/") ;print a[1]}'`
export ORIENTDB_NODE_NAME=`hostname | awk '{split($1,a,".") ;print a[1]}'`
export JAVA_OPTS="-DORIENTDB_HOST=$ORIENTDB_HOST -DORIENTDB_NODE_NAME=$ORIENTDB_NODE_NAME"
${0/%dserver.sh/server.sh} -Ddistributed=true $*
