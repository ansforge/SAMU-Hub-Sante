<template>
  <v-row justify="center">
    <v-col cols="12" sm="7">
      <v-card style="height: 86vh; overflow-y: auto;">
        <v-card-title class="text-h5 d-flex align-center">
          Formulaire
          <source-selector @source-changed="source=$event" />
        </v-card-title>
        <v-card-text>
          <v-tabs
            v-model="messageTypeTabIndex"
            align-tabs="title"
          >
            <v-tabs color="primary" />
            <v-tab
              v-for="{label} in store.messageTypes"
              :key="label"
            >
              {{ label }}
            </v-tab>
          </v-tabs>
          <v-window v-model="messageTypeTabIndex">
            <v-window-item
              v-for="messageTypeDetails in store.messageTypes"
              :key="messageTypeDetails.label"
            >
              <schema-form :ref="'schemaForm_' + messageTypeDetails.label" :source="source" v-bind="messageTypeDetails" no-send-button />
            </v-window-item>
          </v-window>
        </v-card-text>
      </v-card>
    </v-col>
    <v-col cols="12" sm="5">
      <v-card style="height: 86vh; overflow-y: auto;">
        <v-card-title class="text-h5">
          Json live view
          <v-spacer />
          <v-btn primary @click="saveMessage">
            <v-icon start>
              mdi-file-download-outline
            </v-icon>
            Enregistrer
          </v-btn>
          <v-btn secondary @click="validateMessage">
            <v-icon start>
              mdi-text-box-check-outline
            </v-icon>
            Valider
          </v-btn>
        </v-card-title>
        <v-card-text>
          <json-viewer
            v-if="store.currentMessage"
            :value="trimEmptyValues({[currentMessageType?.schema?.title]: store.currentMessage})"
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
import Ajv from 'ajv'
import { useNuxtApp } from 'nuxt/app'
import { REPOSITORY_URL } from '@/constants'
import mixinMessage from '~/mixins/mixinMessage'
import { useMainStore } from '~/store'

// import { mapGetters } from 'pinia';
export default {
  name: 'JsonCreator',
  mixins: [mixinMessage],
  data () {
    return {
      toasts: [],
      store: useMainStore(),
      ajv: new Ajv({
        allErrors: true,
        strict: false
      }),
      source: null,
      mounted: false,
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
    currentMessageType () {
      return this.store.messageTypes[this.messageTypeTabIndex]
    }
  },
  watch: {
    source () {
      this.updateForm()
    },
    currentMessageType () {
      this.store.selectedSchema = this.store.messageTypes[this.messageTypeTabIndex].label
    }
  },
  mounted () {
    this.mounted = true
  },
  methods: {
    updateForm () {
      // To automatically generate the UI and input fields based on the JSON Schema
      // We need to wait the acquisition of 'messagesList' before attempting to acquire the schemas
      this.store.loadMessageTypes(REPOSITORY_URL + this.source + '/src/main/resources/sample/examples/messagesList.json').then(
        () => this.store.loadSchemas(REPOSITORY_URL + this.source + '/src/main/resources/json-schema/')
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
      this.store.currentMessage = form
    },
    validateMessage () {
      for (const toastId of this.toasts) {
        useNuxtApp().$toast.remove(toastId)
      }
      const validationResult = this.validateJson(this.trimEmptyValues(this.store.currentMessage))
      if (validationResult) {
        // Toast all errors, showing instance path at the start of the line
        this.toasts.push(useNuxtApp().$toast.error(
          validationResult.map(error => `${error.instancePath}/: ${error.message}`).join('<br>')
        ))
      } else {
        this.toasts.push(useNuxtApp().$toast.success(
          'Le message est valide'
        ))
      }
    },
    saveMessage () {
      // Download as file | Ref.: https://stackoverflow.com/a/34156339
      // JSON pretty-print | Ref.: https://stackoverflow.com/a/7220510
      const data = JSON.stringify(this.trimEmptyValues({ [this.currentMessageType?.schema?.title]: this.store.currentMessage }), null, 2)
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
