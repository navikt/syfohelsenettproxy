FROM gcr.io/distroless/java25-debian13@sha256:00cb938f4bae711dee23dab0b2fdb1ed36a5a242283193e49ec05d271ad8263c
WORKDIR /app
COPY build/install/*/lib /lib
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
ENTRYPOINT ["java", "-cp", "/lib/*", "no.nav.syfo.ApplicationKt"]
