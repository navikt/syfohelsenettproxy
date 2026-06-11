FROM gcr.io/distroless/java25-debian13@sha256:202ae7b10d0929fec7590927e5d14fad9b9a9b886ead6df4c555b8ae9ebeec17
WORKDIR /app
COPY build/install/*/lib /lib
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
ENTRYPOINT ["java", "-cp", "/lib/*", "no.nav.syfo.ApplicationKt"]
