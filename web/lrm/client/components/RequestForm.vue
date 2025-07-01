<template>
  <v-form v-model="valid">
    <vjsf v-model="localMessage" :schema="processedSchema" :options="options" />
  </v-form>
</template>

<script setup>
import { ref, watch, computed } from 'vue';
import Vjsf from '@koumoul/vjsf';
import moment from 'moment';
import { useMainStore } from '~/store';

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
