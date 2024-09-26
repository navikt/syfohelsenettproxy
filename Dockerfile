FROM gcr.io/distroless/java21-debian12@sha256:b262cb56f921e2f2363d95a79be99fb6e9145126baddf145fe9eef0e3d918947
WORKDIR /app
COPY build/libs/app-*.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback.xml"
ENV TZ="Europe/Oslo"
EXPOSE 8080
USER nonroot
CMD ["app.jar"]
