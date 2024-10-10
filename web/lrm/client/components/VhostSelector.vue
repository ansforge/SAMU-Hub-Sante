<template>
  <v-combobox
    v-model="selectedVhost"
    :items="vhosts"
    item-title="vhost"
    label="vHost"
    class="ml-4 pl-4"
    density="compact"
    hide-details
    variant="outlined"
  >
    <template #item="{ props, item }">
      <v-list-item v-bind="props" :subtitle="item.raw.version" />
    </template>"
  </v-combobox>
</template>

<script setup>
import { useMainStore } from '~/store'
const store = useMainStore()
const config = useRuntimeConfig()

const { selectedVhost } = toRefs(store)
const vhosts = Object.keys(config.public.vhostMap).map(vhost => ({ vhost, version: config.public.vhostMap[vhost] }))

</script>
