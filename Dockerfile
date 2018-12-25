FROM oracle/graalvm-ce:1.0.0-rc10
COPY target/s3web*.jar s3web.jar
USER 1000
ENTRYPOINT ["/bin/sh", "-c"]
CMD ["java -jar s3web.jar"]