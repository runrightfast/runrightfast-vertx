### How to create an OrientDB docker container

            docker create --name orientdb-1  \
                    -v /home/alfio/Documents/work/data/orientdb-1/config:/orientdb/config \
                    -v /home/alfio/Documents/work/data/orientdb-1/databases:/orientdb/databases \
                    -v /home/alfio/Documents/work/orientdb-1/data/backup:/orientdb/backup \
                    8fde5be53c84


            docker create --name orientdb-2  \
                    -v /home/alfio/Documents/work/data/orientdb-2/config:/orientdb/config \
                    -v /home/alfio/Documents/work/data/orientdb-2/databases:/orientdb/databases \
                    -v /home/alfio/Documents/work/data/orientdb-2/backup:/orientdb/backup \
                    8fde5be53c84

altered dserver.sh:

            export ORIENTDB_HOST=`ip -4 -o addr show dev ethwe 2> /dev/null | awk '{split($4,a,"/") ;print a[1]}'`
            export ORIENTDB_NODE_NAME=`hostname | awk '{split($1,a,".") ;print a[1]}'`
            ${0/%dserver.sh/server.sh} -Ddistributed=true -DORIENTDB_HOST=$ORIENTDB_HOST -DORIENTDB_NODE_NAME=$ORIENTDB_NODE_NAME $*