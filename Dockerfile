FROM gradle:7-jdk11-alpine AS build

COPY --chown=gradle:gradle *.gradle /home/gradle/src/
COPY --chown=gradle:gradle src /home/gradle/src/src

WORKDIR /home/gradle/src
RUN gradle build --no-daemon

FROM openjdk:11-jre-slim AS run

EXPOSE 8080

RUN mkdir /app

COPY --from=build /home/gradle/src/build/libs/SAMU-Hub-Sante.jar /app/SAMU-Hub-Sante.jar

ENTRYPOINT ["java", "-jar", "/app/SAMU-Hub-Sante.jar"]
