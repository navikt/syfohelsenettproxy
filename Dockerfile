FROM gcr.io/distroless/java21-debian12@sha256:d6ba76b612098d03aa8f0782295c859a7b24476528f96401ca4bdf5bfe38161f
WORKDIR /app
COPY build/libs/app-*.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD ["app.jar"]
