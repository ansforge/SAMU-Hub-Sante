<template>
  <v-combobox
    v-model="selectedSource"
    data-cy="source-selector"
    :items="sources"
    label="Source des schémas"
    class="ml-4 pl-4"
    density="compact"
    hide-details
    variant="outlined"
    :return-object="false"
    @update:model-value="sourceSelected"
  />
  <v-btn
    v-if="currentSchemaOnGitHub()"
    icon
    color="primary"
    variant="text"
    :href="currentSchemaOnGitHub()"
    target="_blank"
  >
    <v-icon>mdi-open-in-new</v-icon>
  </v-btn>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useMainStore } from '~/store';
import { REPOSITORY_URL } from '@/constants';

const store = useMainStore();

const props = defineProps({
  branchNames: {
    type: Array,
    required: true,
  },
});

onMounted(() => {
  selectedSource.value = store.selectedVhost.modelVersion;
  emit('sourceChanged', selectedSource.value);
});

const sources = computed(() => [
  ...new Set(store.vhostMap.map((vhost) => vhost.modelVersion)),
  ...props.branchNames,
]);

const selectedSource = ref('');
const emit = defineEmits(['sourceChanged']);

function sourceSelected() {
  emit('sourceChanged', selectedSource.value);
}

computed(() => {
  return {
    currentSchemaOnGitHub() {
      return currentSchemaOnGitHub();
    },
  };
});
function currentSchemaOnGitHub() {
  return (
    REPOSITORY_URL.replace(
      'https://raw.githubusercontent.com/',
      'https://github.com/'
    ).replace('SAMU-Hub-Modeles/', 'SAMU-Hub-Modeles/tree/') +
    selectedSource.value +
    '/src/main/resources/json-schema/' +
    store.selectedSchema.schemaName
  );
}
</script>
