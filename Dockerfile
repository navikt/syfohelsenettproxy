FROM gcr.io/distroless/java17-debian11@sha256:68e2373f7bef9486c08356bd9ffd3b40b56e6b9316c5f6885eb58b1d9093b43d
WORKDIR /app
COPY build/libs/app-*.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD ["app.jar"]
