FROM oracle/graalvm-ce:1.0.0-rc10 AS staging
COPY target/s3web*.jar s3web.jar
COPY src/main/build/graal-reflect-config.json reflect.json
RUN java -cp s3web.jar io.micronaut.graal.reflect.GraalClassLoadingAnalyzer
RUN native-image \
    --static \
    --no-server \
    --allow-incomplete-classpath \
    --class-path s3web.jar \
    -H:ReflectionConfigurationFiles=reflect.json \
    -H:EnableURLProtocols=http \
    -H:IncludeResources="logback.xml|application.yml|META-INF/services/*.*" \
    -H:Name=s3web \
    -H:Class=uk.me.krupa.s3web.Application \
    -H:+ReportUnsupportedElementsAtRuntime \
    -H:+AllowVMInspection \
    --rerun-class-initialization-at-runtime='sun.security.jca.JCAUtil$CachedSecureRandomHolder,javax.net.ssl.SSLContext' \
    --delay-class-initialization-to-runtime=io.netty.handler.codec.http.HttpObjectEncoder,io.netty.handler.codec.http.websocketx.WebSocket00FrameEncoder,io.netty.handler.ssl.util.ThreadLocalInsecureRandom  \
    -H:-UseServiceLoaderFeature


FROM debian:stable-slim
COPY --from=staging s3web s3web
RUN chmod 755 s3web
ENTRYPOINT ["/bin/sh", "-c"]
CMD "./s3web"