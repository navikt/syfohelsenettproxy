FROM gcr.io/distroless/java17-debian11@sha256:d1ebe3d183e2e6bd09d4fd8f2cf0206693a3bca1858afe393ceb3161b5268f40
WORKDIR /app
COPY build/libs/app-*.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD ["app.jar"]
