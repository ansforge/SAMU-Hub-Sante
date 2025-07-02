<template>
  <v-form v-model="valid">
    <vjsf v-model="localMessage" :schema="processedSchema" :options="options" />
  </v-form>
</template>

<script setup>
import { ref, watch, computed, toRefs } from 'vue';
import Vjsf from '@koumoul/vjsf';
import moment from 'moment';
import { useMainStore } from '~/store';
import {
  findPathsWithSubstring,
  generateTimestampId,
  getChangedPaths,
  getValueAtPath,
  replaceSubstringsAtPaths,
} from '~/composables/messageUtils';
import consola from 'consola';

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
const localMessage = ref(JSON.parse(JSON.stringify(store.currentMessage)));

// Sync changes back to store
watch(
  localMessage,
  (newValue) => {
    store.currentMessage = newValue;
  },
  { deep: true }
);

// Handle initial value changes
watch(
  () => store.currentMessage,
  (newValue) => {
    if (JSON.stringify(newValue) !== JSON.stringify(localMessage.value)) {
      localMessage.value = JSON.parse(JSON.stringify(newValue));
    }
  },
  { immediate: true }
);

const options = ref({
  locale: 'fr',
  defaultLocale: 'fr',
  rootDisplay: 'tabs',
  editMode: 'inline',
  expansionPanelsProps: { mandatory: false },
  density: 'compact',
  debounceInputMs: 500,
  updateOn: 'input',
  validateOn: 'input',
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

const newId = ref(null);
const paths = ref([]);
const { currentMessage } = toRefs(store);

watch(
  () => store.currentMessageLoaded,
  (newValue) => {
    if (newValue) {
      newId.value = generateTimestampId();
    } else {
      consola.warn('Current message not loaded yet.');
    }
  },
  { immediate: true }
);

watch(newId, (newVal) => {
  if (!newVal) return;
  const oldId = currentMessage?.value?.senderCaseId;
  const newId = newVal;
  consola.log('Old case ID:', oldId, 'New case ID:', newId);
  if (!oldId || !newId || oldId === newId) return;

  paths.value = findPathsWithSubstring(currentMessage.value, oldId);
  consola.log('Paths found:', paths.value);
  const newObj = replaceSubstringsAtPaths(
    currentMessage.value,
    paths.value,
    oldId,
    newId
  );

  currentMessage.value = newObj;
});

watch(
  () => currentMessage.value,
  (newValue, oldValue) => {
    const changedPaths = getChangedPaths(newValue, oldValue);
    if (changedPaths.length !== 1) return;
    const changedPath = changedPaths[0];
    const changedValue = getValueAtPath(newValue, changedPath);
    const originalValue = getValueAtPath(oldValue, changedPath);
    const originalCaseId =
      changedPath[0] === 'senderCaseId'
        ? originalValue
        : currentMessage.value?.senderCaseId;
    // TODO: handle case where change are made in the form

    console.log('chagedValue:', changedValue);
    console.log('originalValue:', originalValue);
    console.log('originalCaseId:', originalCaseId);
    // i want paths.value excluding changedPath

    //change id with the new one :
    ///console.log('newId:', newId.value);
  },
  { immediate: true }
);
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
