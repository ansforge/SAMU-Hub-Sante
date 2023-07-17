<template>
  <v-row justify="center">
    <v-col cols="12" md="7">
      <v-card style="height: 86vh; overflow-y: auto;">
        <v-card-title class="headline">Formulaire</v-card-title>
        <v-card-text>
          <v-alert type="info" dense class="mb-0">
            Ce formulaire permet d'envoyer des requêtes vers le bandeau SI-SAMU
          </v-alert>
          <v-card-title>Infos cœur</v-card-title>
          <v-form>
            <v-row>
              <v-col
                cols="12"
                md="6"
              >
                <v-combobox
                  v-model="form.urlSiSamu"
                  :items="items.urlSiSamu"
                  label="URL SI-SAMU"
                  hide-details="auto"
                  dense
                ></v-combobox>
              </v-col>
              <v-col
                cols="12"
                md="6"
              >
                <v-combobox
                  v-model="form.idCrra"
                  :items="items.idCrra"
                  label="idCrra"
                  dense
                ></v-combobox>
              </v-col>
            </v-row>
            <v-row>
              <v-col
                cols="12"
                md="6"
              >
                <v-combobox
                  v-model="form.idNatPs"
                  :items="items.idNatPs"
                  label="idNatPs"
                  dense
                ></v-combobox>
              </v-col>
              <v-col
                cols="12"
                md="6"
              >
                <v-combobox
                  v-model="form.idDossier"
                  :items="items.idDossier"
                  label="idDossier"
                  hide-details="auto"
                  dense
                ></v-combobox>
              </v-col>
            </v-row>
          </v-form>
          <RequestForm
            v-if="swagger"
            v-for="(requestInfos, request) in requests"
            :key="request"
            :swagger="swagger"
            :completeForm="form"
            :requestInfos="requestInfos"
            :items="items"
            v-model="form"
            @submit="submit(request)"
            @save="save(request)"
          />
        </v-card-text>
      </v-card>
    </v-col>
    <v-col cols="12" md="5">
      <v-card style="height: 86vh; overflow-y: auto;">
        <v-card-title class="headline">
          Messages
          <v-badge :content="messages.length"/>
        </v-card-title>
        <v-card-text>
          <transition-group name="message">
            <div v-for="{direction, endpoint, time, receivedTime, code, body, from} in messages" :key="time"
                 class="message mb-4">
              <v-badge :color="code === 200 ? 'green' : 'red'" :content="code"></v-badge>

              <pre style="white-space: pre-wrap; background-color: rgba(0, 0, 0, 0.05);"
                   class="elevation-1 pa-2 mt-n3"><span v-if="from">{{ from }}<br></span>{{ direction }}{{
                  endpoint
                }}<br>{{ time }} -> {{ receivedTime }}<br>{{
                  body
                }}</pre>
            </div>
          </transition-group>

        </v-card-text>
      </v-card>
    </v-col>
  </v-row>
</template>

<script>
export default {
  name: 'IndexPage',
  head() {
    return {
      title: 'Interface SI-SAMU tester',
    }
  },
  data() {
    return {
      swagger: null,
      messages: [{
        direction: '→ ',
        endpoint: '/',
        time: this.time(),
        receivedTime: this.time(),
        code: 200,
        body: {body: 'Page loaded successfully!'}
      }],
      requests: {
        clickToCall: {
          name: 'Click-to-Call',
          actionName: 'Appeler',
          properties: [],
          justCopiedToClipboard: false
        },
        correlation: {
          name: 'Corrélation',
          actionName: 'Corréler',
          properties: [],
          justCopiedToClipboard: false
        },
      },
      items: {
        urlSiSamu: ['http://localhost:8080', 'https://www.portail.vft.si-samu.fr', 'https://www.portail.testsutilisateurs.si-samu.fr', 'https://www.portail.preprod.si-samu.fr', 'https://www.portail.formation.si-samu.fr'],
        idCrra: ['FR090', 'FR42A', 'FR870', 'FR080', 'FR530'],
        idNatPs: ['00B9814506', '1234', '518751275100020/0000000613'],
        numTel: ['0606060606'],
        idDossier: ['22298003', 'idDossier', '2301236789'],
        idAppel: ['interne-SI-SAMU'],
        idFlux: ['FR090-FluxStd-MU-P0-F02'],
        prioriteRegul: ['P0', 'P1', 'P2', 'P3', 'NR'],
        localisation: ['PariSanté Campus', 'Tour Pitard'],
        appelant: {
          nom: ['Appelant'],
          prenom: ['Jean'],
          adresse: ['1 rue de la paix']
        },
        patients: {
          nom: ['Patient'],
          nomNaissance: ['Patient Bébé'],
          prenom: ['Michel'],
          sexe: ['M', 'F', 'O', 'U'],
          age: ['P75Y', 'P9M'],
          motifRecours: ['Motif Recours', 'AUTCHUTE']
        }
      },
      form: {
        urlSiSamu: 'https://www.portail.vft.si-samu.fr',
        idCrra: 'FR090',
        idNatPs: '00B9814506',
        numTel: '0606060606',
        idDossier: '2301236789',
        idAppel: 'interne-SI-SAMU',
        idFlux: 'FR090-FluxStd-MU-P0-F02',
        prioriteRegul: 'P2',
        localisation: 'PariSanté Campus',
        appelant: {
          nom: 'Appelant',
          prenom: 'Jean',
          adresse: '1 rue de la paix'
        }
      }
    }
  },
  mounted() {
    // To automatically generate the UI and input fields based on the swagger
    fetch('swagger-si-samu.json')
      .then((response) => response.json())
      .then((swagger) => {
        this.swagger = swagger;
        console.log(swagger);
        // Collecting properties for each request
        this.requests.clickToCall.properties = swagger.definitions.DemandeAppelSortant.properties;
        this.requests.correlation.properties = swagger.definitions.CorrelationDossier.properties;
      });
    // Start listening to server events
    this.longPolling();
  },
  methods: {
    async longPolling() {
      this.$axios.$get('/poll', {timeout: 10000}).then((response) => {
        try {
          response.body = JSON.parse(response.body);
        } catch (e) {
        }
        this.messages.unshift({
          ...response,
          direction: '→ ',
          receivedTime: this.time()
        });
        this.longPolling();
      }).catch(error => {
        console.log(error)
        this.longPolling();
      })
    },
    time() {
      const d = new Date();
      return d.toLocaleTimeString().replace(':', 'h') + '.' + d.getMilliseconds();
    },
    getSpecificValues(request) {
      return Object.fromEntries(
        Object.keys(this.requests[request].properties)
          .filter(key => key in this.form)
          .map(key => [key, this.form[key]])
      );
    },
    getCallData(request) {
      let endpoint, caller, method;
      if (request === 'clickToCall') {
        endpoint = '/si-samu-back-synchro-lrm-web/api/v1/cti/' + this.form.idCrra + '/agents/' + this.form.idNatPs.replace('/', '%2F') + '/calls';
        caller = this.$axios.$post;
        method = 'POST'
      } else if (request === 'correlation') {
        endpoint = '/si-samu-back-synchro-lrm-web/api/v1/cti/' + this.form.idCrra + '/dossiers/' + this.form.idDossier;
        caller = this.$axios.$put;
        method = 'PUT';
      } else {
        console.error('Unknown request', request);
      }
      return {
        caller,
        method,
        endpoint: this.form.urlSiSamu + endpoint,
        data: this.getSpecificValues(request)
      };
    },
    save(request) {
      const {method, endpoint, data} = this.getCallData(request);
      const curlRequest = `curl --key certif.key --cert certif.crt -v \\
                    -X ${method} -k -H 'Content-Type: application/json' \\
                    -i '${endpoint}' --data '${JSON.stringify(data)}'`;
      console.log(curlRequest);
      // Copy to clipboard
      navigator.clipboard.writeText(curlRequest);
      // Toggle copied icon
      this.requests[request].justCopiedToClipboard = true;
      setTimeout(() => this.requests[request].justCopiedToClipboard = false, 1000);
    },
    submit(request) {
      const {caller, endpoint, data} = this.getCallData(request);
      const time = this.time();
      // Could be using Swagger generated client, but it would validate fields!
      caller(
        '/forward',
        {endpoint, data}
      ).then((response) => {
        console.log(response);
        this.messages.unshift({
          direction: '← ',
          endpoint,
          time,
          receivedTime: this.time(),
          code: 200,
          body: response
        });
      }).catch(error => {
        this.messages.unshift({
          direction: '← ',
          endpoint,
          time,
          receivedTime: this.time(),
          code: error.response.status,
          body: error.response.data
        });
      })
    }
  }
}
</script>
<style>
.message {
  transition: all 0.5s;
}

.message-enter, .message-leave-to
  /* .message-leave-active for <2.1.8 */
{
  opacity: 0;
  transform: scale(0.7) translateY(-500px);
}

.message-enter-to {
  opacity: 1;
  transform: scale(1);
}

.message-leave-active {
  /*position: absolute;*/
}

.message-move {
  opacity: 1;
  transition: all 0.5s;
}
</style>
