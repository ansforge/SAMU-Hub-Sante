<template>
  <v-combobox
    data-cy="source-selector"
    v-model="selectedSource"
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

import { computed, onMounted } from 'vue'
import { useMainStore } from '~/store'
import { REPOSITORY_URL } from '@/constants'

const store = useMainStore()

onMounted(() => {
  selectedSource.value = store.selectedVhost.modelVersion
  emit('sourceChanged', selectedSource.value)
})

const sources = [
  ...new Set(store.vhostMap.map(vhost => vhost.modelVersion)),
  'main',
  'develop',
  'auto/model_tracker',
  '{branchName}'
]

const selectedSource = ref('')
const emit = defineEmits(['sourceChanged'])

function sourceSelected () {
  emit('sourceChanged', selectedSource.value)
}

computed(() => {
  return {
    currentSchemaOnGitHub () {
      return currentSchemaOnGitHub()
    }
  }
})
function currentSchemaOnGitHub () {
  return REPOSITORY_URL.replace(
    'https://raw.githubusercontent.com/', 'https://github.com/'
  ).replace(
    'SAMU-Hub-Modeles/', 'SAMU-Hub-Modeles/tree/'
  ) + store.selectedVhost.modelVersion + '/src/main/resources/json-schema/' + store.selectedSchema.schemaName
}

</script>
