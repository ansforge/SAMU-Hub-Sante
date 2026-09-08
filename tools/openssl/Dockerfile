# Dockerfile for OpenSSL and OpenJDK 11 JRE on Alpine 3.21.3
FROM alpine:3.24.1

RUN apk add --no-cache openssl openjdk11-jre

# Set JAVA_HOME for convenience
ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk
ENV PATH="$JAVA_HOME/bin:$PATH"

CMD ["sh"]
