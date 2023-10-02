FROM gcr.io/distroless/java17-debian11@sha256:052076466984fd56979c15a9c3b7433262b0ad9aae55bc0c53d1da8ffdd829c3
WORKDIR /app
COPY build/libs/app-*.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD ["app.jar"]
