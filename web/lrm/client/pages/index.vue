<template>
  <v-row justify="center">
    <v-col cols="12" md="7">
      <v-card style="height: 86vh; overflow-y: auto;">
        <v-card-title class="headline">
          Formulaire
        </v-card-title>
        <v-card-text>
          <v-alert type="info" dense class="mb-0">
            Ce formulaire permet d'envoyer des messages via le Hub Santé
          </v-alert>
          <v-card-title>Infos cœur</v-card-title>
          <v-form>
            <v-row>
              <v-col
                cols="12"
                md="6"
              >
                <v-combobox
                  v-model="form.clientId"
                  :items="items.clientId"
                  label="Identification"
                  hide-details="auto"
                  dense
                />
              </v-col>
              <v-col
                cols="12"
                md="6"
              >
                <v-combobox
                  v-model="form.routingKey"
                  :items="items.routingKey"
                  label="routingKey"
                  dense
                />
              </v-col>
            </v-row>
          </v-form>
          <RequestForm
            v-for="(requestInfos, request) in requests"
            v-if="swagger"
            :key="request"
            v-model="form"
            :swagger="swagger"
            :complete-form="form"
            :request-infos="requestInfos"
            :items="items"
            @submit="submit(request)"
          />
        </v-card-text>
      </v-card>
    </v-col>
    <v-col cols="12" md="5">
      <v-card style="height: 86vh; overflow-y: auto;">
        <v-card-title class="headline">
          Messages
          <v-badge
            v-if="showedMessages.length"
            :content="showedMessages.length"
          />
          <v-spacer />
          <v-switch
            v-model="config.showSentMessages"
            inset
            :label="'Show sent (' + messagesSentCount + ')'"
          />
        </v-card-title>
        <v-btn-toggle
          v-model="selectedClientId"
          class="ml-4"
          dense
          borderless
          mandatory
        >
          <v-btn v-for="clientId in items.clientId" :key="clientId" :value="clientId">
            <v-icon left>
              {{ clientInfos(clientId).icon }}
            </v-icon>
            <span class="hidden-sm-and-down">
              {{ clientInfos(clientId).name }}
              <v-badge
                v-if="clientMessages(clientId).length > 0"
                class="mr-4 ml-1"
                :content="clientMessages(clientId).length"
              />
            </span>
          </v-btn>
        </v-btn-toggle>
        <v-card-text>
          <transition-group name="message">
            <div
              v-for="{direction, routingKey, time, receivedTime, code, body} in selectedMessages"
              :key="time"
              class="message mb-4"
            >
              <v-badge :color="code === 200 ? 'green' : 'red'" :content="code" />
              <pre
                style="white-space: pre-wrap; background-color: rgba(0, 0, 0, 0.05);"
                class="elevation-1 pa-2 mt-n3"
              ><span v-if="isOut(direction)">{{ direction }} {{
                routingKey
              }}</span><span v-else>{{ direction }} {{
                body.senderID
              }}</span><br>{{ time }} -> {{ receivedTime }}<br>{{
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
const DIRECTIONS = {
  IN: '←',
  OUT: '→'
}
export default {
  name: 'IndexPage',
  data () {
    return {
      swagger: null,
      messages: [/* {
        direction: DIRECTIONS.IN,
        routingKey: '',
        time: this.time(),
        receivedTime: this.time(),
        code: 200,
        body: { body: 'Page loaded successfully!' }
      } */],
      config: {
        showSentMessages: false
      },
      selectedClientId: null,
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
        }
      },
      items: {
        clientId: ['fr.health.samuA', 'fr.health.samuB', 'fr.fire.nexsis.sdisZ'],
        routingKey: ['fr.health.samuA.in.message', 'fr.health.samuB.in.message', 'fr.fire.nexsis.sdisZ.in.ack'],
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
        clientId: 'fr.health.samuA',
        routingKey: 'fr.health.samuB.in.message',
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
  head () {
    return {
      title: 'Interface SI-SAMU tester'
    }
  },
  computed: {
    showedMessages () {
      return this.config.showSentMessages ? this.messages : this.messages.filter(message => !this.isOut(message.direction))
    },
    messagesSentCount () {
      return this.messages.filter(message => this.isOut(message.direction)).length
    },
    selectedMessages () {
      return this.clientMessages(this.selectedClientId)
    }
  },
  mounted () {
    // To automatically generate the UI and input fields based on the swagger
    fetch('swagger-si-samu.json')
      .then(response => response.json())
      .then((swagger) => {
        this.swagger = swagger
        // Collecting properties for each request
        this.requests.clickToCall.properties = swagger.definitions.DemandeAppelSortant.properties
        this.requests.correlation.properties = swagger.definitions.CorrelationDossier.properties
      })
    // Start listening to server events
    this.longPolling()
  },
  methods: {
    longPolling () {
      this.$axios.$get('/poll', { timeout: 10000 }).then((response) => {
        this.messages.unshift({
          ...response,
          direction: DIRECTIONS.IN,
          receivedTime: this.time()
        })
        this.longPolling()
      }).catch((error) => {
        if (error.code === 'ECONNABORTED') {
          console.info('Long polling expiration, restarting.', error)
          this.longPolling()
        } else if (error.message === 'Network Error') {
          console.warn('Server unavailable, waiting before reconnection.', error)
          setTimeout(() => this.longPolling(), 300)
        } else {
          console.error('Error while reading message, restarting.', error)
          this.longPolling()
        }
      })
    },
    time () {
      const d = new Date()
      return d.toLocaleTimeString().replace(':', 'h') + '.' + d.getMilliseconds()
    },
    isOut (direction) {
      return direction === DIRECTIONS.OUT
    },
    clientMessages (clientId) {
      return this.messages.filter(
        message => message.routingKey.startsWith(clientId) && !this.isOut(message.direction)
      ).concat(
        this.config.showSentMessages
          ? this.messages.filter(
            message => message.body.senderID === clientId && this.isOut(message.direction)
          )
          : []
      )
    },
    clientInfos (clientId) {
      return {
        name: clientId.split('.')[2],
        icon: clientId.split('.')[1] === 'health' ? 'mdi-heart-pulse' : 'mdi-fire'
      }
    },
    getSpecificValues (request) {
      return Object.fromEntries(
        Object.keys(this.requests[request].properties)
          .filter(key => key in this.form)
          .map(key => [key, this.form[key]])
      )
    },
    async submit (request) {
      const time = this.time()
      const data = await (await fetch('samuA_to_samuB.json')).json()
      console.log('submit', request, data)
      // Could be done using Swagger generated client, but it would validate fields!
      this.$axios.$post(
        '/publish',
        { key: this.form.clientId, msg: data },
        { timeout: 1000 }
      ).then(() => {
        this.messages.unshift({
          direction: DIRECTIONS.OUT,
          routingKey: this.form.routingKey,
          time,
          code: 200,
          body: data
        })
      }).catch((error) => {
        console.error("Erreur lors de l'envoi du message", error)
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
