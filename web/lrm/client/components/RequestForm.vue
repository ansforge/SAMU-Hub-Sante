<template>
  <div style="display: flex; align-items: center; gap: 16px">
    <v-text-field
      v-model="currentMessageSenderCaseId"
      label="ID du dossier"
    ></v-text-field>
    <v-btn color="secondary" @click="generateId"> Régénérer l'ID </v-btn>
    <v-btn color="primary" @click="replaceId"> Appliquer l'ID</v-btn>
  </div>
  <v-form v-model="valid">
    <vjsf v-model="localMessage" :schema="processedSchema" :options="options" />
  </v-form>
</template>

<script setup>
import { ref, computed, toRefs } from 'vue';
import Vjsf from '@koumoul/vjsf';
import moment from 'moment';
import { useMainStore } from '~/store';
import {
  findPathsWithSubstring,
  generateTimestampId,
  replaceSubstringsAtPaths,
} from '~/composables/messageUtils';

const props = defineProps({
  value: {
    type: Object,
    default: () => ({}),
  },
  schema: {
    type: Object,
    required: true,
  },
  noSendButton: {
    type: Boolean,
    default: false,
  },
});

const store = useMainStore();
const valid = ref(false);

const localMessage = computed({
  get: () => store.currentMessage,
  set: (value) => (store.currentMessage = value),
});

const options = ref({
  locale: 'fr',
  defaultLocale: 'fr',
  rootDisplay: 'tabs',
  editMode: 'inline',
  expansionPanelsProps: { mandatory: false },
  density: 'compact',
  debounceInputMs: 50000,
  updateOn: 'blur',
  validateOn: 'blur',
  ajvOptions: {
    allErrors: true,
    strict: false,
    strictSchema: false,
  },
  formats: {
    'date-time': (dateTime, _locale) => moment(new Date(dateTime)).format(),
  },
});

const processedSchema = computed(() => {
  const schemaCopy = JSON.parse(JSON.stringify(props.schema));
  delete schemaCopy.$schema;
  delete schemaCopy.$id;
  return schemaCopy;
});

const paths = ref([]);
const { currentMessage, currentMessageSenderCaseId } = toRefs(store);

const generateId = () => {
  currentMessageSenderCaseId.value = generateTimestampId();
};

const replaceId = () => {
  if (!currentMessageSenderCaseId.value) return;

  const oldId =
    currentMessage.value?.senderCaseId ??
    currentMessage.value.caseId?.split('.').pop();
  const newId = currentMessageSenderCaseId.value;

  if (!oldId || !newId) return;

  paths.value = findPathsWithSubstring(currentMessage.value, oldId);

  const newObj = replaceSubstringsAtPaths(
    currentMessage.value,
    paths.value,
    oldId,
    newId
  );

  currentMessage.value = newObj;
};
</script>

<style>
.v-application div.vjsf-array-header {
  margin-bottom: 28px !important;
}

.v-application div.vjsf-array {
  margin-bottom: 12px !important;
}

.vjsf-tree > div > div.mb-4.mt-4 {
  display: none;
}

.vjsf .v-alert.bg-error:first-child {
  display: none;
}
</style>
