FROM adoptopenjdk/openjdk11:jdk-11.0.2.9-slim
RUN mkdir -p /tmp/uploads
ADD ./uploads/file-not-found.png /tmp/uploads
WORKDIR /opt
ENV PORT 8080
EXPOSE 8080
COPY target/*.jar /opt/app.jar
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar
