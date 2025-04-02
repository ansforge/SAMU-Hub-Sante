<template>
  <v-row justify="center">
    <v-col cols="12" sm="7">
      <v-card style="height: 86vh; overflow-y: auto">
        <v-card-title class="text-h5 d-flex align-center">
          Formulaire
          <source-selector
            :branch-names="branchesNames"
            @source-changed="source = $event"
          />
          <v-btn
            icon="mdi-reload"
            density="compact"
            :loading="jsonMessagesLoading"
            @click="updateForm"
          />
        </v-card-title>
        <v-card-text>
          <v-tabs
            v-model="messageTypeTabIndex"
            data-cy="message-type-tabs"
            show-arrows
            align-tabs="title"
          >
            <v-tabs color="primary" />
            <v-tab v-for="{ label } in store.messageTypes" :key="label">
              {{ label }}
            </v-tab>
          </v-tabs>
          <v-window v-if="currentMessageType" v-model="messageTypeTabIndex">
            <schema-form
              v-if="currentMessageType"
              :source="source"
              :current-message-type="currentMessageType"
              :message-type-tab-index="messageTypeTabIndex"
              no-send-button
            />
          </v-window>
        </v-card-text>
      </v-card>
    </v-col>
    <v-col cols="12" sm="5">
      <v-card style="height: 86vh; overflow-y: auto">
        <v-card-title class="text-h5">
          Json live view
          <v-spacer />
          <v-btn primary @click="saveMessage">
            <v-icon start> mdi-file-download-outline </v-icon>
            Enregistrer
          </v-btn>
          <v-btn secondary @click="validateMessage">
            <v-icon start> mdi-text-box-check-outline </v-icon>
            Valider
          </v-btn>
          <v-btn
            v-if="store.isAdvanced"
            color="surface-variant"
            variant="flat"
            @click="dialog = true"
          >
            <v-icon start> mdi-github </v-icon>
            Commit
          </v-btn>
          <v-dialog v-model="dialog" max-width="500">
            <v-card title="Commit les changements">
              <template #append>
                <v-btn
                  icon="mdi-close"
                  density="comfortable"
                  variant="text"
                  @click="
                    dialog = false;
                    resetCommitModal();
                  "
                ></v-btn>
              </template>
              <v-card-text>
                <v-switch
                  v-model="createNewBranch"
                  color="primary"
                  label="Utiliser une nouvelle branche ?"
                />
                <v-text-field
                  v-if="createNewBranch"
                  v-model="source"
                  readonly
                  label="Branche de base selectionnée"
                />
                <v-text-field
                  v-if="createNewBranch"
                  v-model="newBranch"
                  label="Nom de la nouvelle branche"
                  :prefix="VALID_BRANCH_PREFIX"
                />
                <v-text-field
                  v-else
                  v-model="source"
                  readonly
                  label="Branche existante selectionnée"
                />
                <v-text-field
                  v-model="adminPassword"
                  type="password"
                  label="Mot de passe administrateur"
                  width="75%"
                  density="compact"
                  prepend-inner-icon="mdi-lock-outline"
                />
              </v-card-text>
              <v-card-actions>
                <v-btn
                  v-if="openedPullRequestLink"
                  color="primary"
                  variant="text"
                  :href="openedPullRequestLink"
                  target="_blank"
                >
                  Open pull request
                </v-btn>
                <v-btn
                  variant="flat"
                  color="surface-variant"
                  :loading="isCommiting"
                  @click="commitChanges"
                >
                  Commit
                </v-btn>
              </v-card-actions>
            </v-card>
          </v-dialog>
        </v-card-title>
        <v-card-text>
          <json-viewer
            v-if="store.currentMessage"
            :value="
              trimEmptyValues({
                [currentMessageType?.schema?.title]: store.currentMessage,
              })
            "
            :expand-depth="10"
            :copyable="{
              copyText: 'Copier',
              copiedText: 'Copié !',
              timeout: 1000,
            }"
            theme="json-theme"
          />
        </v-card-text>
      </v-card>
    </v-col>
  </v-row>
</template>

<script setup>
import Ajv from 'ajv';
import { useNuxtApp } from 'nuxt/app';
import { REPOSITORY_URL } from '@/constants';
import mixinWebsocket from '~/mixins/mixinWebsocket';
import { trimEmptyValues } from '~/composables/messageUtils';
import { useMainStore } from '~/store';
import { isEnvProd } from '~/composables/envUtils';

// eslint-disable-next-line no-undef
useHead({
  title: 'Json Creator - Hub Santé',
});
</script>

<script>
import { consola } from 'consola';

const VALID_BRANCH_PREFIX = 'auto-json-creator/';

export default {
  name: 'JsonCreator',
  mixins: [mixinWebsocket],
  data() {
    return {
      app: useNuxtApp(),
      toasts: [],
      store: useMainStore(),
      ajv: new Ajv({
        code: { es5: true },
        allErrors: true,
        strict: false,
      }),
      source: null,
      messageTypeTabIndex: null,
      currentMessage: null,
      selectedMessageType: 'message',
      selectedClientId: null,
      selectedCaseIds: [],
      queueTypes: [
        {
          name: 'Message',
          type: 'message',
          icon: 'mdi-message',
        },
        {
          name: 'Ack',
          type: 'ack',
          icon: 'mdi-check',
        },
        {
          name: 'Info',
          type: 'info',
          icon: 'mdi-information',
        },
      ],
      form: {},
      jsonMessagesLoading: false,
      branchesNames: [],
      newBranch: '',
      createNewBranch: false,
      isCommiting: false,
      openedPullRequestLink: '',
      adminPassword: '',
      dialog: false,
    };
  },
  computed: {
    currentMessageType() {
      return this.store.messageTypes[this.messageTypeTabIndex];
    },
  },
  watch: {
    source() {
      this.updateForm();
    },
    currentMessageType() {
      this.store.selectedSchema =
        this.store.messageTypes[this.messageTypeTabIndex];
    },
  },
  mounted() {
    this.fetchBranchesNames();
  },
  methods: {
    updateForm() {
      this.jsonMessagesLoading = true;
      // To automatically generate the UI and input fields based on the JSON Schema
      // We need to wait the acquisition of 'messagesList' before attempting to acquire the schemas
      this.store
        .loadMessageTypes(
          REPOSITORY_URL +
            this.source +
            '/src/main/resources/sample/examples/messagesList.json'
        )
        .then(() =>
          this.store
            .loadSchemas(
              REPOSITORY_URL + this.source + '/src/main/resources/json-schema/'
            )
            .then(() => {
              this.messageTypeTabIndex = 0;
            })
            .catch((reason) => {
              consola.error(reason);
              this.toasts.push(
                this.app.$toast.error(
                  "Erreur lors de l'acquisition des schémas de version " +
                    this.source
                )
              );
            })
        )
        .catch(() => {
          consola.error("Couldn't get messagesList.json");
          this.clearToasts();
          this.toasts.push(
            this.app.$toast.error(
              "Erreur lors de l'acquisition de la liste des schémas de version " +
                this.source
            )
          );
        })
        .finally(() => {
          this.jsonMessagesLoading = false;
        });
    },
    useSchema(schema) {
      // We empty the cache since all out schemas have the same $id and we can't add duplicate id schemas to the cache
      for (const key in this.ajv.schemas) {
        this.ajv.removeSchema(key);
        this.ajv.removeKeyword(key);
      }
      for (const key in this.ajv.refs) {
        delete this.ajv.refs[key];
      }
      // We do not validate the schema itself due to ajv being very strict on several points (e.g. uniqueness in 'required' properties) which are not mandatory
      this.ajv.addSchema(schema, schema.title, undefined, false);
    },
    validateJson(json) {
      this.useSchema(this.currentMessageType.schema);
      // Then we validate using the schema
      try {
        this.ajv.validate(this.currentMessageType.schema.title, json);
        return this.ajv.errors;
      } catch (error) {
        return [
          {
            instancePath: '',
            message: 'Un problème est survenu lors de la validation du message',
          },
        ];
      }
    },
    getServerUrl() {
      return `${isEnvProd() ? 'https' : 'http'}://${
        this.$config.public.backendLrmServer
      }/lrm/api`;
    },
    async fetchBranchesNames() {
      // eslint-disable-next-line no-undef
      this.branchesNames = await $fetch(
        `${this.getServerUrl()}/modeles/branches`
      );
    },
    updateCurrentMessage(form) {
      this.store.currentMessage = form;
    },
    clearToasts() {
      for (const toastId of this.toasts) {
        this.app.$toast.remove(toastId);
      }
    },
    validateMessage() {
      this.clearToasts();
      const validationResult = this.validateJson(
        trimEmptyValues(this.store.currentMessage)
      );
      if (validationResult) {
        // Toast all errors, showing instance path at the start of the line
        this.toasts.push(
          this.app.$toast.error(
            validationResult
              .map((error) => `${error.instancePath}/: ${error.message}`)
              .join('<br>')
          )
        );
      } else {
        this.toasts.push(this.app.$toast.success('Le message est valide'));
      }
    },
    saveMessage() {
      // Download as file | Ref.: https://stackoverflow.com/a/34156339
      // JSON pretty-print | Ref.: https://stackoverflow.com/a/7220510
      const data = JSON.stringify(
        trimEmptyValues({
          [this.currentMessageType?.schema?.title]: this.store.currentMessage,
        }),
        null,
        2
      );
      const a = document.createElement('a');
      const file = new Blob([data], { type: 'application/json' });
      a.href = URL.createObjectURL(file);
      a.download = `${this.currentMessageType?.label}-message.json`;
      a.click();
    },
    async commitChanges() {
      const data = JSON.stringify(
        trimEmptyValues({
          [this.currentMessageType?.schema?.title]: this.store.currentMessage,
        }),
        null,
        2
      );
      this.isCommiting = true;
      console.log('examples:', this.currentMessageType.examples);
      console.log('current message:', this.store.currentMessage);
      try {
        // eslint-disable-next-line no-undef
        const commitResponse = await $fetch(`${this.getServerUrl()}/modeles`, {
          method: 'POST',
          body: JSON.stringify({
            password: this.adminPassword,
            fileName:
              this.currentMessageType.examples[this.messageTypeTabIndex].file,
            content: data,
            branchConfig: this.createNewBranch
              ? {
                  isNewBranch: true,
                  baseBranch: this.source,
                  branch: `${VALID_BRANCH_PREFIX}${this.newBranch}`,
                }
              : {
                  isNewBranch: false,
                  branch: this.source,
                },
          }),
        });
        this.openedPullRequestLink = commitResponse.data.pull_request_url;
        this.toasts.push(this.app.$toast.success('Le commit a été effectué.'));
      } catch (err) {
        const errorMessage =
          err?.data?.message || 'Une erreur inattendue est survenue.';
        this.toasts.push(this.app.$toast.error(errorMessage));
      } finally {
        this.isCommiting = false;
      }
    },
    resetCommitModal() {
      this.dalog = false;
      this.adminPassword = '';
      this.newBranch = '';
      this.createNewBranch = false;
    },
  },
};
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

  .jv-button {
    color: #49b3ff;
  }

  .jv-key {
    color: #111111;
  }

  .jv-item {
    &.jv-array {
      color: #111111;
    }

    &.jv-boolean {
      color: #fc1e70;
    }

    &.jv-function {
      color: #067bca;
    }

    &.jv-number {
      color: #fc1e70;
    }

    &.jv-number-float {
      color: #fc1e70;
    }

    &.jv-number-integer {
      color: #fc1e70;
    }

    &.jv-object {
      color: #111111;
    }

    &.jv-undefined {
      color: #e08331;
    }

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
