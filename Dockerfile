FROM gcr.io/distroless/java21-debian12@sha256:1ff8b923c0696dad3462f6f1f10f22481477d9daecde63e008de09d523d15c2b
WORKDIR /app
COPY build/libs/app-*.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD ["app.jar"]
