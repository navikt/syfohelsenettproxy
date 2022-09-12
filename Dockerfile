FROM navikt/java:17
COPY build/libs/syfohelsenettproxy-*-all.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
