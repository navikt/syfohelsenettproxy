FROM gcr.io/distroless/java21-debian12@sha256:914d2e4d0aef6afe6167a11de8d87a4bfcd9325f36d1b45c03c04e6f16ba94d8
WORKDIR /app
COPY build/libs/*.jar ./
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD [ "app-1.0.0.jar" ]
