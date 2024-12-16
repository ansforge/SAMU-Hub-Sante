## Hub Santé
This directory contains all the code needed for the Hub Santé itself (RabbitMQ and Dispatcher).

### Local development
To run the Dispatched locally:
- Generate a custom `application-XXX.properties` file in `dispatcher/src/main/resources`:
```bash
cp dispatcher/src/main/resources/application-rfo.properties dispatcher/src/main/resources/application-XXX.properties
```
and update the paths according to your local setup & update the trigram in the filename.
- Generate a [client.preferences.csv file](dispatcher/src/main/resources/client.preferences.csv):
```bash
echo "client_id;useXML" > dispatcher/src/main/resources/client.preferences.csv
```
- In your terminal, set the `GITHUB_ACTOR` env variable to your Github username, and `GITHUB_TOKEN` with the value of a Github Token with full `repo` and `read:package` permissions. To [generate a token](https://github.com/settings/tokens/new).
- In the `hub/dispatcher` folder, run `gradle bootRun --args='--spring.profiles.active=local,XXX'` (replace XXX with the correct trigram)

To run the RabbitMQ or/and Dispatcher locally through Kubernetes and Docker, go to [infra/](./infra/README.md)
