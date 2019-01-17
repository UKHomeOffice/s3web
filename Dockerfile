FROM oracle/graalvm-ce:1.0.0-rc10
COPY target/s3web*.jar s3web.jar
USER 1000
ENV JAVA_OPTS=""
ENTRYPOINT ["/bin/sh", "-c"]
CMD ["java ${JAVA_OPTS} -jar s3web.jar"]