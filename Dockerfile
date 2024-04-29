FROM gcr.io/distroless/java17-debian11@sha256:0276f5c6b7c10db8aaf1d54986dcce49df3b27eb79bf7a2a62deed4b9607b134
WORKDIR /app
COPY build/libs/app-*.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD ["app.jar"]
