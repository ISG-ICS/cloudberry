FROM anapsix/alpine-java

RUN apk add --update unzip wget curl docker jq coreutils

ENV KAFKA_VERSION="0.10.1.0" SCALA_VERSION="2.11"
ADD download-kafka.sh /tmp/download-kafka.sh
RUN chmod a+x /tmp/download-kafka.sh && sync && /tmp/download-kafka.sh && tar xfz /tmp/kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz -C /opt && rm /tmp/kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz

ENV KAFKA_HOME /opt/kafka_${SCALA_VERSION}-${KAFKA_VERSION}

ADD start-kafka.sh /usr/bin/start-kafka.sh
# The scripts need to have executable permission
RUN chmod a+x /usr/bin/start-kafka.sh
# Use "exec" form so that it runs as PID 1 (useful for graceful shutdown)
ENTRYPOINT ["start-kafka.sh"]
