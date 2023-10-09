FROM gcr.io/distroless/java17-debian11@sha256:17459919f2ccb3439155da0b4f42ddf08cb022b2a3e1a4b9491d425dfcc31e7e
WORKDIR /app
COPY build/libs/app-*.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD ["app.jar"]
