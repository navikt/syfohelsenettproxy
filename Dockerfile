FROM gcr.io/distroless/java21-debian12@sha256:b9094cce11d2baac4b6bc51bb2d218c9fb60170655be5d6250a8143e0cde2621
WORKDIR /app
COPY build/libs/app-*.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD ["app.jar"]
