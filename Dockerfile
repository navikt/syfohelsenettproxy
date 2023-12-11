FROM gcr.io/distroless/java17-debian11@sha256:3147bb05aca27b48e7b9042444488a127e3f8a256c8534a277791375a1b105a9
WORKDIR /app
COPY build/libs/app-*.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD ["app.jar"]
