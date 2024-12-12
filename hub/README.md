## Hub Santé
This directory contains all the code needed for the Hub Santé itself (RabbitMQ and Dispatcher).

### Local development
To run the Dispatched locally:
- Create an [application-XXX.properties file](dispatcher/src/main/resources/application-rfo.properties) with at least the following content :
```
spring.rabbitmq.ssl.key-store=file:<path/to/this/repo>/certs/dispatcher.tmp.p12
spring.rabbitmq.ssl.trust-store=file:<path/to/this/repo>/certs/trustStore

dispatcher.vhost="15-15_v1.5"

client.preferences.file=file:<path/to/this/repo>/hub/dispatcher/src/main/resources/client.preferences.csv
```
- Create a [client.preferences.csv file](dispatcher/src/main/resources/client.preferences.csv) with the following content :
```
client_id;useXML
```
- In your terminal, set the `GITHUB_ACTOR` env variable to your Github username, and `GITHUB_TOKEN` with the value of a Github Token with full `repo` and `read:package` permissions. To [generate a token](https://github.com/settings/tokens/new).
- In the `hub/dispatcher` folder, run `gradle bootRun --args='--spring.profiles.active=local,XXX'`

To run the RabbitMQ or/and Dispatcher locally through Kubernetes and Docker, go to [infra/](./infra/README.md)
