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
      <v-list-item v-bind="props" :subtitle="item.raw.modelVersion" />
    </template>"
  </v-combobox>
</template>

<script setup>
import { useMainStore } from '~/store'
const store = useMainStore()

const { selectedVhost } = toRefs(store)
const vhosts = store.vhostMap

onMounted(() => {
  selectedVhost.value = store.selectedVhost
})

</script>
