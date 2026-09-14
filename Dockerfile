FROM gcr.io/distroless/java25-debian13@sha256:1d7a0cea4653f62be34a5b9b1da82a4dd097ae8935d1d3f4ab84146e0396fd2b
WORKDIR /app
COPY build/install/*/lib /lib
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
ENTRYPOINT ["java", "-cp", "/lib/*", "no.nav.syfo.ApplicationKt"]
