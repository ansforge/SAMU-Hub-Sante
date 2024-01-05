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
            :return-object="false"
          />
          <v-btn
            v-if="currentSchemaOnGitHub"
            icon
            color="primary"
            :href="currentSchemaOnGitHub"
            target="_blank"
          >
            <v-icon>mdi-open-in-new</v-icon>
          </v-btn>
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
        createCaseCisu: {
          label: 'RC-EDA',
          schemaName: 'RC-EDA.schema.json',
          schema: null,
          examples: [{
            file: 'RC-EDA-usecase-Armaury-1.json',
            icon: 'mdi-bike-fast',
            name: 'Alexandre ARMAURY',
            caller: 'Albane Armaury, témoin accident impliquant son mari,  Alexandre Armaury',
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
            file: 'EMSI-DC-usecase-Armaury-2.json',
            icon: 'mdi-bike-fast',
            name: 'Alexandre ARMAURY (DC)',
            caller: 'Albane Armaury, témoin accident impliquant son mari,  Alexandre Armaury',
            context: 'Collision de 2 vélos',
            environment: 'Voie cyclable à Lyon, gêne de la circulation',
            victims: '2 victimes, 1 nécessitant assistance SAMU',
            victim: 'Homme, adulte, 43 ans',
            medicalSituation: 'Céphalées, migraines, traumatismes sérieux, plaies intermédiaires'
          }, {
            file: 'EMSI-EO-usecase-Armaury-3.json',
            icon: 'mdi-bike-fast',
            name: 'Alexandre ARMAURY (RDC)',
            caller: 'Albane Armaury, témoin accident impliquant son mari,  Alexandre Armaury',
            context: 'Collision de 2 vélos',
            environment: 'Voie cyclable à Lyon, gêne de la circulation',
            victims: '2 victimes, 1 nécessitant assistance SAMU',
            victim: 'Homme, adulte, 43 ans',
            medicalSituation: 'Céphalées, migraines, traumatismes sérieux, plaies intermédiaires'
          }]
        },
        createCase: {
          label: 'RS-EDA',
          schemaName: 'RS-EDA.schema.json',
          schema: null,
          examples: [{
            file: 'RS-EDA-usecase-PartageDossier-1.json',
            icon: 'mdi-circular-saw',
            name: 'Didier Morel',
            caller: 'Sébastien Morel, témoin accident impliquant son père, Didier Morel',
            context: 'Accident domestique : blessure grave causée par une scie circulaire électrique',
            environment: 'Domicile, outil scie débranché et sécurisé',
            victims: '1 victime, nécessitant assistance SAMU',
            victim: 'Homme, adulte, 65 ans',
            medicalSituation: 'Plaie traumatique profonde, perte de conscience, hémorragie importante'
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
    },
    currentSchemaOnGitHub () {
      if (this.selectedSource.includes('https://raw.githubusercontent.com/')) {
        return this.selectedSource.replace(
          'https://raw.githubusercontent.com/', 'https://github.com/'
        ).replace(
          'SAMU-Hub-Modeles/', 'SAMU-Hub-Modeles/tree/'
        ) + this.$refs.schemaForms[this.messageTypeTabIndex].schemaName
      }
      return false
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
        console.log('Loading schema from: ' + this.selectedSource + schemaName)
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

<style lang="scss">
// Ref.: https://github.com/chenfengjw163/vue-json-viewer/tree/master#theming
// values are default one from jv-light template
.json-theme {
  background: rgba(0, 0, 0, 0);
  white-space: nowrap;
  color: #525252;
  font-size: 14px;
  font-family: Consolas, Menlo, Courier, monospace;

  .jv-ellipsis {
    color: #999;
    background-color: #eee;
    display: inline-block;
    line-height: 0.9;
    font-size: 0.9em;
    padding: 0px 4px 2px 4px;
    border-radius: 3px;
    vertical-align: 2px;
    cursor: pointer;
    user-select: none;
  }
  .jv-button { color: #49b3ff }
  .jv-key { color: #111111 }
  .jv-item {
    &.jv-array { color: #111111 }
    &.jv-boolean { color: #fc1e70 }
    &.jv-function { color: #067bca }
    &.jv-number { color: #fc1e70 }
    &.jv-number-float { color: #fc1e70 }
    &.jv-number-integer { color: #fc1e70 }
    &.jv-object { color: #111111 }
    &.jv-undefined { color: #e08331 }
    &.jv-string {
      color: #42b983;
      word-break: break-word;
      white-space: normal;
    }
  }
  .jv-code {
    padding-bottom: 12px !important;
    .jv-toggle {
      &:before {
        padding: 0px 2px;
        border-radius: 2px;
      }
      &:hover {
        &:before {
          background: #eee;
        }
      }
    }
  }
}
</style>
