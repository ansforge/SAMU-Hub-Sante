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
              v-for="{label} in messageTypes"
              :key="label"
            >
              {{ label }}
            </v-tab>
          </v-tabs>
          <v-tabs-items v-model="messageTypeTabIndex">
            <v-tab-item
              v-for="messageTypeDetails in messageTypes"
              :key="messageTypeDetails.label"
            >
              <SchemaForm :ref="'schemaForm_' + messageTypeDetails.label" v-bind="messageTypeDetails" no-send-button @on-form-update="updateCurrentMessage" />
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
          <v-btn secondary @click="validateMessage">
            <v-icon left>
              mdi-text-box-check-outline
            </v-icon>
            Valider
          </v-btn>
        </v-card-title>
        <v-card-text>
          <json-viewer
            v-if="currentMessage"
            :value="trimEmptyValues({[currentMessageType?.schema?.title]: currentMessage})"
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
import Ajv from 'ajv'
import mixinMessage from '~/plugins/mixinMessage'
import { REPOSITORY_URL } from '@/constants'
const config = useRuntimeConfig()

export default {
  name: 'JsonCreator',
  mixins: [mixinMessage],
  data () {
    return {
      ajv: new Ajv({
        allErrors: true,
        strict: false
      }),
      mounted: false,
      selectedSource: REPOSITORY_URL + config.public.modelBranch + '/src/main/resources/',
      sources: [{
        divider: true,
        header: 'GitHub branches'
      }, {
        text: 'main',
        value: REPOSITORY_URL + 'main/src/main/resources/'
      }, {
        text: 'develop',
        value: REPOSITORY_URL + 'develop/src/main/resources/'
      }, {
        text: 'auto/model_tracker',
        value: REPOSITORY_URL + 'auto/model_tracker/src/main/resources/'
      }, {
        divider: true,
        header: 'Templates'
      }, {
        text: REPOSITORY_URL + '{branchName}/src/main/resources/',
        value: REPOSITORY_URL + '{branchName}/src/main/resources/'
      }],
      messageTypeTabIndex: 0,
      currentMessage: null,
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
    ...mapGetters(['messages', 'isAdvanced', 'messageTypes']),
    currentMessageType () {
      return this.messageTypes[this.messageTypeTabIndex]
    },
    currentSchemaForm () {
      // Ref.: https://stackoverflow.com/a/43531779
      if (this.mounted) {
        // $refs is array (in v-for) and non reactive | Ref.: https://v2.vuejs.org/v2/api/#ref
        return this.$refs['schemaForm_' + this.currentMessageType?.label]?.[0]
      }
      return null
    },
    currentSchemaOnGitHub () {
      if (this.selectedSource.includes('https://raw.githubusercontent.com/')) {
        return this.selectedSource.replace(
          'https://raw.githubusercontent.com/', 'https://github.com/'
        ).replace(
          'SAMU-Hub-Modeles/', 'SAMU-Hub-Modeles/tree/'
        ) + this.currentSchemaForm?.schemaName
      }
      return false
    }
  },
  watch: {
    currentSchemaForm () {
      this.currentMessage = this.currentSchemaForm?.form
    },
    selectedSource () {
      this.updateForm()
    }
  },
  mounted () {
    this.updateForm()
    this.mounted = true
  },
  methods: {
    updateForm () {
      // To automatically generate the UI and input fields based on the JSON Schema
      // We need to wait the acquisition of 'messagesList' before attempting to acquire the schemas
      this.$store.dispatch('loadMessageTypes', this.selectedSource+"/sample/examples/messagesList.json").then(
        () => this.$store.dispatch('loadSchemas', this.selectedSource+"/json-schema/")
      )
    },
    useSchema (schema) {
      // We empty the cache since all out schemas have the same $id and we can't add duplicate id schemas to the cache
      for (const key in this.ajv.schemas) {
        this.ajv.removeSchema(key)
        this.ajv.removeKeyword(key)
      }
      for (const key in this.ajv.refs) {
        delete this.ajv.refs[key]
      }
      // We do not validate the schema itself due to ajv being very strict on several points (e.g. uniqueness in 'required' properties) which are not mandatory
      this.ajv.addSchema(schema, schema.title, undefined, false)
    },
    validateJson (json) {
      this.useSchema(this.currentMessageType.schema)
      // Then we validate using the schema
      this.ajv.validate(this.currentMessageType.schema.title, json)
      return this.ajv.errors
    },
    updateCurrentMessage (form) {
      this.currentMessage = form
    },
    validateMessage () {
      const validationResult = this.validateJson(this.trimEmptyValues(this.currentMessage))
      if (validationResult) {
        // Toast all errors, showing instance path at the start of the line
        this.$toast.error(validationResult.map(error => `${error.instancePath}/: ${error.message}`).join('<br>'))
      } else {
        this.$toast.success('Message valide')
      }
    },
    saveMessage () {
      // Download as file | Ref.: https://stackoverflow.com/a/34156339
      // JSON pretty-print | Ref.: https://stackoverflow.com/a/7220510
      const data = JSON.stringify(this.trimEmptyValues({ [this.currentMessageType?.schema?.title]: this.currentMessage }), null, 2)
      const a = document.createElement('a')
      const file = new Blob([data], { type: 'application/json' })
      a.href = URL.createObjectURL(file)
      a.download = `${this.currentMessageType?.label}-message.json`
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
