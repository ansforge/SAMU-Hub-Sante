<template>
  <v-row justify="center">
    <v-col cols="12" sm="7">
      <v-card style="height: 86vh; overflow-y: auto;">
        <v-card-title class="headline">
          Formulaire
          <v-combobox
            v-model="selectedSource"
            :items="sources"
            label="Source des schémas"
            class="ml-4 pl-4"
            dense
            hide-details
            outlined
          />
        </v-card-title>
        <v-card-text>
          <v-tabs
            v-model="messageTypeTabIndex"
            align-with-title
          >
            <v-tabs-slider color="primary" />
            <v-tab
              v-for="[name, {label}] in Object.entries(messageTypes)"
              :key="name"
            >
              {{ label }}
            </v-tab>
          </v-tabs>
          <v-tabs-items v-model="messageTypeTabIndex">
            <v-tab-item
              v-for="[name, messageTypeDetails] in Object.entries(messageTypes)"
              :key="name"
            >
              <SchemaForm v-bind="messageTypeDetails" ref="schemaForms" :name="name" no-send-button />
            </v-tab-item>
          </v-tabs-items>
        </v-card-text>
      </v-card>
    </v-col>
    <v-col cols="12" sm="5">
      <v-card style="height: 86vh; overflow-y: auto;">
        <v-card-title class="headline">
          Json live view
          <v-spacer />
          <v-btn primary @click="saveMessage">
            <v-icon left>
              mdi-file-download-outline
            </v-icon>
            Enregistrer
          </v-btn>
        </v-card-title>
        <v-card-text>
          <json-viewer
            v-if="currentMessage"
            :value="currentMessage"
            :expand-depth="10"
            :copyable="{copyText: 'Copier', copiedText: 'Copié !', timeout: 1000}"
            theme="json-theme"
          />
        </v-card-text>
      </v-card>
    </v-col>
  </v-row>
</template>

<script>
import { mapGetters } from 'vuex'
import mixinMessage from '~/plugins/mixinMessage'

export default {
  name: 'JsonCreator',
  mixins: [mixinMessage],
  data () {
    return {
      mounted: false,
      selectedSource: 'schemas/',
      sources: [{
        text: 'Local',
        value: 'schemas/'
      }, {
        divider: true,
        header: 'GitHub branches'
      }, {
        text: 'main',
        value: 'https://raw.githubusercontent.com/ansforge/SAMU-Hub-Modeles/main/src/main/resources/json-schema/'
      }, {
        text: 'auto/model_tracker',
        value: 'https://raw.githubusercontent.com/ansforge/SAMU-Hub-Modeles/auto/model_tracker/src/main/resources/json-schema/'
      }, {
        divider: true,
        header: 'Templates'
      }, {
        text: 'https://raw.githubusercontent.com/ansforge/SAMU-Hub-Modeles/{branchName}/src/main/resources/json-schema/',
        value: 'https://raw.githubusercontent.com/ansforge/SAMU-Hub-Modeles/{branchName}/src/main/resources/json-schema/'
      }],
      messageTypeTabIndex: 0,
      // ToDo: load schemas from github branch directly so it is up to date!
      // ToDo: migrate this to store so they can be reused through pages (demo/ and json/)
      // ToDo: when message are uploaded, add them in store
      // ToDo: when message is loaded, add them in store to not load them again later
      messageTypes: {
        createCase: {
          label: 'RC-EDA',
          schemaName: 'RC-EDA.schema.json',
          schema: null,
          examples: [{
            file: 'RC-EDA Armaury VF.json',
            icon: 'mdi-bike-fast',
            name: 'Alexandre ARMAURY',
            caller: 'Albane Armaury, témoin accident impliquant son mari, Alexandre Armaury',
            context: 'Collision de 2 vélos',
            environment: 'Voie cyclable à Lyon, gêne de la circulation',
            victims: '2 victimes, 1 nécessitant assistance SAMU',
            victim: 'Homme, adulte, 43 ans',
            medicalSituation: 'Céphalées, migraines, traumatismes sérieux, plaies intermédiaires'
          }, {
            file: '../exemple-invalide.json',
            icon: 'mdi-alert-circle-outline',
            name: 'Champs manquants',
            context: "Pour illustrer les messages d'INFO sur les erreurs de validation"
          }]
        },
        emsi: {
          label: 'EMSI',
          schemaName: 'EMSI.schema.json',
          schema: null,
          examples: [{
            file: 'EMSI-DC Armaury VF.json',
            icon: 'mdi-bike-fast',
            name: 'Alexandre ARMAURY (DC)',
            caller: 'Albane Armaury, témoin accident impliquant son mari,  Alexandre Armaury',
            context: 'Collision de 2 vélos',
            environment: 'Voie cyclable à Lyon, gêne de la circulation',
            victims: '2 victimes, 1 nécessitant assistance SAMU',
            victim: 'Homme, adulte, 43 ans',
            medicalSituation: 'Céphalées, migraines, traumatismes sérieux, plaies intermédiaires'
          }, {
            file: 'EMSI-EO Armaury RDC VF.json',
            icon: 'mdi-bike-fast',
            name: 'Alexandre ARMAURY (RDC)',
            caller: 'Albane Armaury, témoin accident impliquant son mari,  Alexandre Armaury',
            context: 'Collision de 2 vélos',
            environment: 'Voie cyclable à Lyon, gêne de la circulation',
            victims: '2 victimes, 1 nécessitant assistance SAMU',
            victim: 'Homme, adulte, 43 ans',
            medicalSituation: 'Céphalées, migraines, traumatismes sérieux, plaies intermédiaires'
          }]
        } /* ,
        info: {
          label: 'RS-INFO',
          schemaName: 'RS-INFO.schema.json',
          schema: null,
          examples: []
        } */
      },
      selectedMessageType: 'message',
      selectedClientId: null,
      selectedCaseIds: [],
      queueTypes: [{
        name: 'Message',
        type: 'message',
        icon: 'mdi-message'
      }, {
        name: 'Ack',
        type: 'ack',
        icon: 'mdi-check'
      }, {
        name: 'Info',
        type: 'info',
        icon: 'mdi-information'
      }],
      form: {}
    }
  },
  head () {
    return {
      title: 'Json Creator'
    }
  },
  computed: {
    ...mapGetters(['messages', 'isAdvanced']),
    currentMessage () {
      if (this.mounted) {
        // Ref.: https://stackoverflow.com/a/43531779
        return this.$refs.schemaForms?.[this.messageTypeTabIndex]?.form
      }
      return null
    }
  },
  watch: {
    selectedSource () {
      this.loadSchemas()
    }
  },
  mounted () {
    // To automatically generate the UI and input fields based on the JSON Schema
    this.loadSchemas()
    this.mounted = true
  },
  methods: {
    loadSchemas () {
      Promise.all(Object.entries(this.messageTypes).map(([name, { schemaName }]) => {
        console.log(this.selectedSource + schemaName)
        return fetch(this.selectedSource + schemaName).then(response => response.json()).then(schema => ({ name, schema }))
      })).then((schemas) => {
        schemas.forEach(({ name, schema }) => {
          this.messageTypes[name].schema = schema
        })
      })
    },
    saveMessage () {
      // Download as file | Ref.: https://stackoverflow.com/a/34156339
      // JSON pretty-print | Ref.: https://stackoverflow.com/a/7220510
      const data = JSON.stringify(this.$refs.schemaForms[this.messageTypeTabIndex].form, null, 2)
      const a = document.createElement('a')
      const file = new Blob([data], { type: 'application/json' })
      a.href = URL.createObjectURL(file)
      a.download = `${this.$refs.schemaForms[this.messageTypeTabIndex].name}-message.json`
      a.click()
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
