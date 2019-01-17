FROM openjdk:8-alpine
COPY target/s3web*.jar s3web.jar
USER 1000
EXPOSE 3000
ENV JAVA_OPTS=""
ENTRYPOINT ["/bin/sh", "-c"]
CMD ["java ${JAVA_OPTS} -jar s3web.jar"]