FROM gcr.io/distroless/java17-debian11@sha256:2f01c2ff0c0db866ed73085cf1bb5437dd162b48526f89c1baa21dd77ebb5e6d
WORKDIR /app
COPY build/libs/app-*.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD ["app.jar"]
