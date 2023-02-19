# Caching Gradle plugins & dep for faster builds | Ref.: https://stackoverflow.com/a/59022743/10115198
FROM gradle:7-jdk11-alpine AS cache
RUN mkdir -p /home/gradle/cache_home
ENV GRADLE_USER_HOME /home/gradle/cache_home
COPY build.gradle /home/gradle/java-code/
WORKDIR /home/gradle/java-code
RUN gradle clean build -i --stacktrace

FROM gradle:7-jdk11-alpine AS build
COPY --from=cache /home/gradle/cache_home /home/gradle/.gradle
COPY *.gradle /home/gradle/java-code/
COPY src /home/gradle/java-code/src

WORKDIR /home/gradle/java-code
RUN gradle build -i --stacktrace

FROM openjdk:11-jre-slim AS run

EXPOSE 8080

RUN mkdir /app

COPY --from=build /home/gradle/java-code/build/libs/SAMU-Hub-Sante.jar /app/SAMU-Hub-Sante.jar

ENTRYPOINT ["java", "-jar", "/app/SAMU-Hub-Sante.jar"]
