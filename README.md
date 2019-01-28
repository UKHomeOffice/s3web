# s3web

Web interface that allows simple HTTP-based GET and PUT access to an S3 bucket with unarchiving capability.

Runs on port 3000.

This application provides no front-end security for either read or write operations and is intended to be put behind
an authenticated gatway such as https://github.com/keycloak/keycloak-gatekeeper.

Accepts uploads of type application/octet-stream, text/plain.  Content-Type for download is based on the file extension.

If the pathname of an upload ends with either .zip or .tar.gz, then the archive will automatically be exploded into a
directory with that name.

The following JVM System Properties are supported

* backend.mode - Optional - can be set to 'stub' to use an in-memory storage backend instead of S3
* aws.s3.region - AWS region containing the S3 bucket
* aws.s3.bucket - Name of the S3 bucket
* aws.access.key.id - Access key ID for the IAM credentials
* aws.secret.access.key - Secret access key for the IAM credentials
* aws.s3.kms.key.id - KMS key ID to use for encryption (optional - KMS will be disabled if this is not specified)
* aws.s3.endpoint - S3 endpoint (optional, used for testing with tools like localstack)
* external.endpoint - Base URL of the application (for WAF/gateway use)
* logging.simple - By default this application uses the Logback Logstash JSON encoder.  Setting this property to
  "true" will use a simpler human-readable log format

Examples:
```
java \
    -Daws.s3.region=eu-west-2 \
    -Daws.s3.bucket=bukkit \
    -Daws.access.key.id=xyzzy \
    -Daws.secret.access.key=plugh \
    -Dlogging.simple \
    -jar target/s3web-1.0.0.jar

curl -X PUT http://localhost:8080/site/site.tar.gz -H"Content-Type: application/octet-stream" --data-binary @./site.tar.gz
```