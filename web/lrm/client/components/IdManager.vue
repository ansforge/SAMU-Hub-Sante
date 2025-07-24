<template>
  <v-expansion-panels v-if="oldId">
    <v-expansion-panel>
      <v-expansion-panel-title>
        🔧 Modification de l'identifiant local du dossier/affaire dans le
        formulaire
      </v-expansion-panel-title>
      <v-expansion-panel-text>
        <div class="id-manager">
          <v-alert
            border="start"
            color="info"
            density="compact"
            variant="tonal"
          >
            Cet identifiant est utilisé dans plusieurs champs du formulaire.
            Modifiez-le ici pour le mettre à jour partout en une seule fois.
          </v-alert>
          <v-text-field
            v-model="newId"
            label="ID du dossier"
            density="compact"
            :rules="[
              (value) =>
                /^[a-zA-Z0-9]*$/.test(value) ||
                'Seuls les caractères alphanumériques sont autorisés',
            ]"
          ></v-text-field>
          <div class="id-manager__btns">
            <v-btn color="primary" variant="outlined" @click="generateId">
              🎲 Générer un nouvel ID
            </v-btn>
            <v-btn
              color="primary"
              :disabled="!currentMessageSenderCaseId"
              @click="replaceId"
            >
              Appliquer l’ID au formulaire
            </v-btn>
          </div>
        </div>
      </v-expansion-panel-text>
    </v-expansion-panel>
  </v-expansion-panels>
</template>

<script setup>
import { computed, ref, toRefs } from 'vue';
import { useMainStore } from '~/store';
import {
  findPathsWithSubstring,
  generateTimestampId,
  replaceSubstringsAtPaths,
} from '~/composables/messageUtils';

const store = useMainStore();
const { currentMessage, currentMessageSenderCaseId } = toRefs(store);
const paths = ref([]);
const oldId = computed(() => {
  return (
    currentMessage.value?.senderCaseId ??
    currentMessage.value?.caseId?.split('.').pop()
  );
});

const newId = computed({
  get: () => currentMessageSenderCaseId.value ?? oldId.value,
  set: (value) => {
    currentMessageSenderCaseId.value = value;
  },
});

const generateId = () => {
  currentMessageSenderCaseId.value = generateTimestampId();
};

const replaceId = () => {
  if (!oldId.value || !newId.value) return;

  paths.value = findPathsWithSubstring(currentMessage.value, oldId.value);

  const newObj = replaceSubstringsAtPaths(
    currentMessage.value,
    paths.value,
    oldId.value,
    newId.value
  );
  currentMessage.value = newObj;
};
</script>

<style scoped>
.id-manager {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.id-manager__btns {
  display: flex;
  gap: 8px;
}
</style>
