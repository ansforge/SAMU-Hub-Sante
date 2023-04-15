## Hub Santé
This directory contains all the code needed for the Hub Santé itself (RabbitMQ and Disptacher).

### Local development
To run it locally, create an [application-XXX.properties file](./src/main/resources/application-rfo.properties) and, in the `hub/` folder, run `gradle bootRun --args='--spring.profiles.active=local,XXX'`
