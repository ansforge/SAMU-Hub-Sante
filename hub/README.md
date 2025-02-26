## Hub Santé
This directory contains all the code needed for the Hub Santé itself (RabbitMQ and Dispatcher).

### Local development
To run the Dispatcher locally with Gradle, create an [application-XXX.properties file](dispatcher/src/main/resources/application-rfo.properties) and, in the `hub/dispatcher` folder, run `gradle bootRun --args='--spring.profiles.active=local,XXX'`
