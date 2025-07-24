<template>
  <div class="id-manager">
    <v-text-field
      v-model="currentMessageSenderCaseId"
      label="ID du dossier"
      density="compact"
      :rules="[
        (value) =>
          /^[a-zA-Z0-9]*$/.test(value) ||
          'Seuls les caractères alphanumériques sont autorisés',
      ]"
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
.id-manager {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 2rem;
}

.v-text-field >>> .v-input__details {
  position: absolute;
}
</style>
