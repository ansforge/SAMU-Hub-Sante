<template>
  <div style="display: flex; align-items: center; gap: 16px">
    <v-text-field
      v-model="currentMessageSenderCaseId"
      label="ID du dossier"
      density="compact"
    ></v-text-field>
    <v-btn color="primary" variant="outlined" @click="generateId">
      Régénérer l'ID
    </v-btn>
    <v-btn
      color="primary"
      :disabled="!currentMessageSenderCaseId"
      @click="replaceId"
    >
      Appliquer l'ID
    </v-btn>
  </div>
</template>

<script setup>
import { ref, toRefs } from 'vue';
import { useMainStore } from '~/store';
import {
  findPathsWithSubstring,
  generateTimestampId,
  replaceSubstringsAtPaths,
} from '~/composables/messageUtils';

const store = useMainStore();
const { currentMessage, currentMessageSenderCaseId } = toRefs(store);
const paths = ref([]);

const generateId = () => {
  currentMessageSenderCaseId.value = generateTimestampId();
};

const replaceId = () => {
  if (!currentMessageSenderCaseId.value) return;

  const oldId =
    currentMessage.value?.senderCaseId ??
    currentMessage.value.caseId?.split('.').pop();
  const newId = currentMessageSenderCaseId.value;

  if (!oldId || !newId) return;

  paths.value = findPathsWithSubstring(currentMessage.value, oldId);

  const newObj = replaceSubstringsAtPaths(
    currentMessage.value,
    paths.value,
    oldId,
    newId
  );

  currentMessage.value = newObj;
};
</script>

<style scoped>
.v-text-field >>> .v-input__details {
  display: none;
}
</style>
