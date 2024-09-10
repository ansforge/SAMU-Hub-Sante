<template>
  <v-combobox
    v-model="selectedSource"
    :items="sources"
    label="Source des schémas"
    class="ml-4 pl-4"
    density="compact"
    hide-details
    variant="outlined"
    :return-object="false"
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

import { computed } from 'vue'
import { useMainStore } from '~/store'
import { REPOSITORY_URL } from '@/constants'

const store = useMainStore()
const config = useRuntimeConfig()

const sources = [
  'main',
  'develop',
  'auto/model_tracker',
  '{branchName}'
]

onMounted(() => {
  if (!store.selectedSource) { store.selectedSource = config.public.modelBranch }
})

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
  ) + store.selectedSource + '/src/main/resources/json-schema/' + store.selectedSchema + '.schema.json'
}

const { selectedSource } = toRefs(store)

</script>
