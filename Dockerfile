FROM ghcr.io/navikt/baseimages/temurin:17
COPY build/libs/syfohelsenettproxy-*-all.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
