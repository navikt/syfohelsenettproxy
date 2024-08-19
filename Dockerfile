FROM gcr.io/distroless/java21-debian12@sha256:6ff4d4d587335e7bd4ab8cf1a6bbe46e68a64ecc8ad6fa10d50ad4cc84deb294
WORKDIR /app
COPY build/libs/app-*.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD ["app.jar"]
