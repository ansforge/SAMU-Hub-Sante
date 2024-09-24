<template>
  <v-combobox
    v-model="selectedVhost"
    :items="vhosts"
    label="Vhost"
    class="ml-4 pl-4"
    density="compact"
    hide-details
    variant="outlined"
    :return-object="false"
    @update:model-value="vhostSelected"
  />
</template>

<script setup>
import { useMainStore } from '~/store'
const store = useMainStore()
const config = useRuntimeConfig()

const { selectedVhost } = toRefs(store)
const vhosts = Object.keys(config.public.vhostMap)

const emit = defineEmits(['vhostChanged'])

onMounted(() => {
  selectedVhost.value = vhosts[0]
  emit('vhostChanged', selectedVhost.value)
})

function vhostSelected () {
  emit('vhostChanged', selectedVhost.value)
}

</script>
