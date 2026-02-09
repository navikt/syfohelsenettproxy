FROM gcr.io/distroless/java21-debian12@sha256:db7c4c75e566f4e0a83efb57e65445a8ec8e2ce0564bb1667cd32ea269cac044
WORKDIR /app
COPY build/install/*/lib /lib
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
ENTRYPOINT ["java", "-cp", "/lib/*", "no.nav.syfo.ApplicationKt"]
