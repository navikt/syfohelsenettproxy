FROM gcr.io/distroless/java17-debian11@sha256:d9fcff2a8b437a7ae055da8797e8ea539236c9fab829646652fc3cc106c93da6
WORKDIR /app
COPY build/libs/app-*.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD ["app.jar"]
