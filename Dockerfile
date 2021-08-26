FROM navikt/java:12
COPY build/libs/syfohelsenettproxy-*-all.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback-remote.xml"
