FROM gcr.io/distroless/java21-debian12@sha256:4c79ab242b99bf7ce29f97be45c0c360e3ecd564475becdfcdcc01a3196777ac
WORKDIR /app
COPY build/libs/app-*.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD ["app.jar"]
