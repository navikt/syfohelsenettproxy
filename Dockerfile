FROM gcr.io/distroless/java21-debian12@sha256:ed5be62a70c5b99708b4ad0fc53bda628d11e46e917f66720fd218cae8fe1568
WORKDIR /app
COPY build/install/*/lib /lib
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
ENTRYPOINT ["java", "-cp", "/lib/*", "no.nav.syfo.ApplicationKt"]
