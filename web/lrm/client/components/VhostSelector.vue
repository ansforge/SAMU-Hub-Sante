<template>
  <span data-cy="vhost-selector">
    <v-combobox
      v-model="selectedVhost"
      :items="vhosts"
      item-title="vhost"
      label="vHost"
      density="compact"
      hide-details
      variant="outlined"
    >
      <template #item="{ props, item }">
        <v-list-item
          v-bind="props"
          :subtitle="item.raw.modelVersion"
        /> </template
      >"
    </v-combobox>
  </span>
</template>

<script setup>
  import { useMainStore } from '~/store';
  import { toRefs, onMounted } from 'vue';
  const store = useMainStore();

  const { selectedVhost } = toRefs(store);
  const vhosts = store.vhostMap;

  onMounted(() => {
    selectedVhost.value = store.selectedVhost;
  });
</script>

<style scoped>
  span {
  flex: 1 1 auto;
}
</style>
