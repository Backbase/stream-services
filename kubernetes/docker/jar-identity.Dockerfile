# https://hub.docker.com/layers/adoptopenjdk/library/adoptopenjdk/11-jre
FROM adoptopenjdk:11-jre

ARG ENV=dbsx

ENV SIG_SECRET_KEY="JWTSecretKeyDontUseInProduction!"
ENV EXTERNAL_SIG_SECRET_KEY="JWTSecretKeyDontUseInProduction!"
ENV EXTERNAL_ENC_SECRET_KEY="JWTEncKeyDontUseInProduction666!"
ENV USERCTX_KEY="JWTSecretKeyDontUseInProduction!"

ENV KEYCLOAK_ADMIN="admin"
ENV KEYCLOAK_ADMIN_PASSWORD="admin"
ENV KEYCLOAK_PASSWORD="admin"
ENV KEYCLOAK_USER="admin"

ENV OPTS="-XX:+UseConcMarkSweepGC \
    -agentlib:jdwp=transport=dt_socket,address=5009,server=y,suspend=n \
    -XX:+CMSClassUnloadingEnabled \
    -Dspring.cloud.kubernetes.enabled=false \
    -javaagent:/ext/elastic-apm-agent-1.20.0.jar \
    -javaagent:/ext/jmx_prometheus_javaagent-0.15.0.jar=8687:prom_config.yaml \
    -Xms1024m \
    -Xmx4096m \
    -Dcom.sun.management.jmxremote \
    -Dcom.sun.management.jmxremote.authenticate=false \
    -Dcom.sun.management.jmxremote.ssl=false \
    -Dcom.sun.management.jmxremote.port=8686 \
    -Dcom.sun.management.jmxremote.local.only=false \
    -Dcom.sun.management.jmxremote.rmi.port=8686 \
    -Djava.rmi.server.hostname=localhost \
    -Dloader.path=/usr/src/app/lib/ \
    -Dlogging.config=config/logback-spring.xml \
    -Dspring.config.location=application.yml \
    -Dkeycloak.migration.action=import \
    -Dkeycloak.profile.feature.upload_scripts=enabled \
    -Djboss.server.config.dir=/tmp/keycloak-export \
    -Dkeycloak.theme.dir=/opt/bb-identity/themes \
    -cp app.jar:/usr/src/app/lib/sqljdbc.jar:/opt/bb-identity/themes/*:/opt/bb-identity/providers/*"

COPY kubernetes/docker/certs/*.crt /usr/local/share/ca-certificates/

RUN chmod 644 /usr/local/share/ca-certificates/*

RUN update-ca-certificates
RUN $JAVA_HOME/bin/keytool -import -noprompt -trustcacerts -alias default -file /usr/local/share/ca-certificates/oly-ca-01.crt -storepass changeit -keystore $JAVA_HOME/lib/security/cacerts
RUN $JAVA_HOME/bin/keytool -import -noprompt -trustcacerts -alias oly-rootca-01-alias -file /usr/local/share/ca-certificates/oly-rootca-01.crt -storepass changeit -keystore $JAVA_HOME/lib/security/cacerts
RUN $JAVA_HOME/bin/keytool -import -noprompt -trustcacerts -alias oly-intca-01-alias -file /usr/local/share/ca-certificates/oly-intca-01.crt -storepass changeit -keystore $JAVA_HOME/lib/security/cacerts


#mTLS import public certificates from other services into this service's truststore
#RUN mkdir /usr/local/share/ca-certificates/mtls
#COPY kubernetes/docker/mtls/certs/arrangements-integration-service.crt /usr/local/share/ca-certificates/mtls
#RUN chmod 755 /usr/local/share/ca-certificates/mtls
#RUN chmod 644 /usr/local/share/ca-certificates/mtls/arrangements-integration-service.crt
#RUN $JAVA_HOME/bin/keytool -import -noprompt -trustcacerts -alias arrangements-integration-service-alias -file /usr/local/share/ca-certificates/mtls/arrangements-integration-service.crt -storepass changeit -keystore $JAVA_HOME/lib/security/cacerts

ARG CAPABILITY
ARG APPLICATION_CONFIG
ARG LOGBACK_SPRING_CONFIG=kubernetes/docker/base/logback-spring.xml

RUN mkdir /ext
COPY kubernetes/docker/mssql-jdbc-9.4.0.jre11.jar /usr/src/app/lib/sqljdbc.jar

COPY ${APPLICATION_CONFIG}/json /tmp/keycloak-export
COPY ${APPLICATION_CONFIG}/themes /opt/bb-identity/themes
COPY ${APPLICATION_CONFIG}/providers /opt/bb-identity/providers

COPY ${APPLICATION_CONFIG}/application.yml /usr/src/app/config/application.yml
COPY ${APPLICATION_CONFIG}/bootstrap.yml /usr/src/app/bootstrap.yml
COPY ${LOGBACK_SPRING_CONFIG} /usr/src/app/config/logback-spring.xml
COPY kubernetes/docker/certs /opt/certs
RUN mkdir -p $JAVA_HOME/lib/security
COPY kubernetes/docker/base/jce $JAVA_HOME/lib/security

RUN cd /ext && curl -LO http://oly-k8srepo-51/devops/elastic-apm/elastic-apm-agent-1.20.0.jar
RUN cd /ext && curl -LO http://oly-k8srepo-51/devops/prometheus-jmx/jmx_prometheus_javaagent-0.15.0.jar
COPY kubernetes/docker/prom_config.yaml /usr/src/app

COPY ${CAPABILITY}.jar /usr/src/app/app.jar
#RUN groupadd -r dbp && useradd --no-log-init -r -g dbp dbp
#RUN chown -R dbp:dbp /usr/src/app
#USER dbp:dbp
WORKDIR /usr/src/app

CMD exec java $OPTS io.quarkus.runner.GeneratedMain
