# runrightfast-vertx-orientdb-server

This project creates a Docker image for an OrientDB server Vertx application.

- run the following Gradle command from the project directory to build the image: 

            ../gradlew build appDocker 

- [SSL](http://orientdb.com/docs/last/Using-SSL-with-OrientDB.html) is enabled for server connections

## Creating the Docker Container
- The Docker container has a dependency on [Weave](http://weave.works/) to run the cluster.
- Docker create container command:

        docker create --name=runrightfast-vertx-orientdb-server-<n> \
            -e "ORIENTDB_ROOT_PASSWORD=<dba_password>" \
            -e "ORIENTDB_KEYSTORE_PASSWORD=<keystore_password>" \
            -e "ORIENTDB_TRUSTSTORE_PASSWORD=<truststore_password>" \
            -v <volume>:/orientdb \
            <image_id>

        e.g.,

        docker create --name=runrightfast-vertx-orientdb-server-1 \            
            -e "ORIENTDB_ROOT_PASSWORD=root" \
            -e "ORIENTDB_KEYSTORE_PASSWORD=qwerty90" \
            -e "ORIENTDB_TRUSTSTORE_PASSWORD=qwerty90" \
            -v /home/alfio/Documents/work/data/orientdb-1:/orientdb \
            <image_id>

        docker create --name=runrightfast-vertx-orientdb-server-2 \            
            -e "ORIENTDB_ROOT_PASSWORD=root" \
            -e "ORIENTDB_KEYSTORE_PASSWORD=qwerty90" \
            -e "ORIENTDB_TRUSTSTORE_PASSWORD=qwerty90" \
            -v /home/alfio/Documents/work/data/orientdb-2:/orientdb \
            <image_id>

        docker create --name=runrightfast-vertx-orientdb-server-3 \
            -e "ORIENTDB_ROOT_PASSWORD=root" \
            -e "ORIENTDB_KEYSTORE_PASSWORD=qwerty90" \
            -e "ORIENTDB_TRUSTSTORE_PASSWORD=qwerty90" \
            -v /home/alfio/Documents/work/data/orientdb-3:/orientdb \
            <image_id>

- **the $ORIENTDB_HOME/databases directory must exist on the /orientdb mounted volume because it is used as the working directory**
        
