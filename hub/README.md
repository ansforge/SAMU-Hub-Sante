## Hub Santé
This directory contains all the code needed for the Hub Santé itself (RabbitMQ and Dispatcher).

### Local development
To run the Dispatched locally:
1. Generate a custom `application-XXX.yaml` file in `dispatcher/src/main/resources`:
```bash
cp dispatcher/src/main/resources/application-XXX.template.yaml dispatcher/src/main/resources/application-XXX.yaml
```
and update the paths according to your local setup & update the trigram in the filename.

2. Generate a [client.preferences.csv file](dispatcher/src/main/resources/client.preferences.csv):
```bash
echo "client_id;useXML" > dispatcher/src/main/resources/client.preferences.csv
```

3. In your terminal, set the `GITHUB_ACTOR` env variable to your Github username, and `GITHUB_TOKEN` with the value of a Github Token with full `repo` and `read:package` permissions. To [generate a token](https://github.com/settings/tokens/new).

4. In the `hub/dispatcher` folder, run `gradle bootRun --args='--spring.profiles.active=local,XXX'` (replace XXX with the correct trigram)

5. Pour lancer les instances du dispatcher s'intégrant dans le setup de dev local :

```bash
# Vhost 15-15-v1
./gradlew bootRun -PmodelVersion=1.3.0 --args='--spring.profiles.active=local,local-15-15-v1,XXX'

# Vhost 15-15-v2
./gradlew bootRun -PmodelVersion=2.4.0 --args='--spring.profiles.active=local,local-15-15-v2,XXX'

# Vhost 15-15-v3
./gradlew bootRun -PmodelVersion=3.3.2 --args='--spring.profiles.active=local,local-15-15-v3,XXX'

# Vhost 15-nexsis
./gradlew bootRun -PmodelVersion=3.3.2 --args='--spring.profiles.active=local,local-15-nexsis,XXX'
```

#### IntelliJ Setup

**Setup SDK:**
- Open the **whole repo** with IntelliJ (CE or Ultimate). Wait for Gradle setup & project indexing.
- Follow the steps 1 & 2 in the above section. Also [generate a token](https://github.com/settings/tokens/new) with full `repo` and `read:package` permissions if you don't have any yet.
- Under "File" select "Projet Structure". Select "Java 21" (Corretto recommanded) as "SDK", and "SDK default" as "Language Level".
- Under "Run" select "Edit Configurations"


**Create Run/Debug configuration:**

*IntelliJ CE :*
- Create a new "Application" configuration named "HubDispatcher"
- "java 21" should be selected in the first dropdown.
- Set the "-cp" option to "SAMU-Hub-Sante.hub.dispatcher.main"
- Set the main class option to "com.hubsante.hub.HubApplication"
- Under "Modify Options" check "Add VM Options"
- Set the VM options to "-Dspring.profiles.active=local,XXX"

*IntelliJ Ultimate :*
- Create a new "Spring Boot" configuration named "HubDispatcher"
- Set runtime as "java 21"
- Set the "-cp" option to "SAMU-Hub-Sante.hub.dispatcher.main"
- Set the main class option to "com.hubsante.hub.HubApplication"
- Set the Active profiles option to "local,XXX"
- Under "Modify Options" check "Environment variables"

**Troubleshooting:**

- Make sure you do not have a conflicting Java version already installed on your machine. You should be using Java 21.

**Run the dispatcher:**
- Add `GITHUB_ACTOR` & `GITHUB_TOKEN` environment variables in the configuration. 
    You can also set them in a gradle.properties file project scoped, based on [this template](dispatcher/gradle.properties.tmpl) (git ignored)
    or globally scoped, located at ~/.gradle/gradle.properties
- Click "Apply" & "Ok"
- Run the application

### Run on Kubernetes locally

To run the RabbitMQ or/and Dispatcher locally through Kubernetes and Docker, go to [infra/](./infra/README.md)

### Format the code

The code is formatted using [Spotless](https://github.com/diffplug/spotless), which is checked during the CI process.
To format it, run the following command in the `hub/dispatcher` folder:
```bash
./gradlew spotlessApply
```

### Format license headers

The license headers are managed by Spotless (configured in [./dispatcher/gradle/spotless.gradle](./dispatcher/gradle/spotless.gradle))
They are applied when running `./gradlew spotlessApply`.

### Message Integrity Testing

To ensure the integrity of the platform and that it does not modify the content of messages, the message is hashed when entering and leaving the dispatcher.
A test located in the `com.hubsante.hub.service.LogIntegrityTest` module verifies that this hash is logged, and allows to replay a message in the dispatcher.
With this test, it is possible to verify that the message provided by the client is exactly the same as the one that passed through the dispatcher by comparing the input hashes.

To perform this comparison, modify the `input_integrity_message` file in the `src/test/resources/sample` folder with the content of the original message. Then modify the `SENDER_ID`, `DISTRIBUTION_ID`, `INPUT_HASH` and `INPUT_MESSAGE_TYPE` attributes of the test file with the values retrieved from the production environment logs.
You can also change the config files (`src/test/resources/config/supported.messages.csv` and `src/test/resources/config/client.preferences.csv`) to match the cluster configuration used in production. 
Finally, specify the virtual host of RabbitMQ to use, at the top of the test file in the SpringBootTest properties.

After modifying these values, run the first test `dispatchLogsHashWhenReceivingMessage`, which should succeed if the input message provided is correct.
This test should output the request to the converter in `src/test/resources/sample/conversion-request-body.json`, if the conversion was required.

If the conversion is needed, you can take the content of this output file, call the converter API and retrieve the JSON response.
Then, replace the content of the `conversion-response-content.json` file in the `src/test/resources/sample` folder with this JSON response.
Finally, run the second test `dispatchLogsHashBeforeSendingMessage`, which should also succeed if the output message matches the one sent in production.

If the conversion is not needed, you can immediatly run the second test `dispatchLogsHashBeforeSendingMessage` which should succeed as well.

Be sure to use the same version of the dispatcher, of the model library (this can be referenced in the `build.gradle`) and of the converter API (if needed) that was used in production.
