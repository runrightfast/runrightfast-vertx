FROM andreptb/oracle-java:8
MAINTAINER Alfio Zappala <alfio.a.z.2@gamil.com>
RUN apt-get update && apt-get upgrade -y && apt-get install -y wget
WORKDIR /
RUN wget "http://orientdb.com/download.php?email=unknown@unknown.com&file=orientdb-community-2.1.1.tar.gz&os=linux" -O orientdb-community-2.1.1.tar.gz &&     tar -zxvf orientdb-community-2.1.1.tar.gz &&     mv orientdb-community-2.1.1 orientdb &&     rm -rf orientdb-community-2.1.1.tar.gz &&     mkdir -p /orientdb/backup
ADD orientdb/bin/dserver.sh /orientdb/bin/
RUN chmod 755 /orientdb/bin/*.sh && rm -rf /var/lib/apt/lists/*
VOLUME ["/orientdb/backup", "/orientdb/databases", "/orientdb/config"]
EXPOSE 2424
WORKDIR /orientdb
ENTRYPOINT ["/orientdb/bin/dserver.sh"]
