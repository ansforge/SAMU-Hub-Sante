<template>
  <v-form v-model="valid">
    <vjsf
      v-model="currentMessage"
      :schema="formatSchema(schemaCopy)"
      :options="options"
    />
  </v-form>
</template>

<script setup>
import { ref, toRefs, computed } from 'vue';
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

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const emit = defineEmits(['submit']);

const store = useMainStore();
const valid = ref(false);
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

const { currentMessage } = toRefs(store);

const schemaCopy = computed(() => JSON.parse(JSON.stringify(props.schema)));

const formatSchema = (schema) => {
  let newSchema = { ...schema };
  // Remove $ props from schema
  newSchema = remove$PropsFromSchema(schema);
  return newSchema;
};

const remove$PropsFromSchema = (schema) => {
  const newSchema = { ...schema };
  delete newSchema.$schema;
  delete newSchema.$id;
  return newSchema;
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
