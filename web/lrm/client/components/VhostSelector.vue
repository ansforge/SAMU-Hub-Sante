<template>
  <span data-cy="vhost-selector">
    <v-combobox
      v-model="selectedVhost"
      :items="vhosts"
      label="vHost"
      class="ml-4 pl-4"
      density="compact"
      hide-details
      variant="outlined"
    />
  </span>
</template>

<script setup>
import { useMainStore } from '~/store'
const store = useMainStore()
const config = useRuntimeConfig()

const { selectedVhost } = toRefs(store)
const vhosts = Object.keys(config.public.vhostMap)

</script>
