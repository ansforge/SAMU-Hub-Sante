<template>
  <v-dialog v-model="dialog" :max-width="maxWidth" persistent>
    <template #activator="{ props: activatorProps }">
      <v-btn
        :prepend-icon="props.btnIcon"
        v-bind="activatorProps"
        variant="outlined"
        color="primary"
        @click="openDialog"
      >
        {{ props.buttonText }}
      </v-btn>
    </template>

    <v-card :prepend-icon="props.icon" :title="props.title">
      <v-card-text>
        {{ props.message }}
      </v-card-text>

      <template #actions>
        <v-spacer></v-spacer>
        <v-btn @click="handleDisagree">
          {{ props.disagreeText }}
        </v-btn>
        <v-btn @click="handleAgree">
          {{ props.agreeText }}
        </v-btn>
      </template>
    </v-card>
  </v-dialog>
</template>

<script setup>
import { ref, defineProps, defineEmits } from 'vue';

const dialog = ref(false);
const props = defineProps({
  title: {
    type: String,
    required: true,
  },
  message: {
    type: String,
    required: true,
  },
  buttonText: {
    type: String,
    default: 'Open Dialog',
  },
  agreeText: {
    type: String,
    default: 'Accepter',
  },
  disagreeText: {
    type: String,
    default: 'Refuser',
  },
  icon: {
    type: String,
    default: '',
  },
  btnIcon: {
    type: String,
    default: '',
  },
  maxWidth: {
    type: String,
    default: '400',
  },
});

const emit = defineEmits(['agree', 'disagree']);

const openDialog = () => {
  dialog.value = true;
};

const handleAgree = () => {
  dialog.value = false;
  emit('agree');
};

const handleDisagree = () => {
  dialog.value = false;
  emit('disagree');
};
</script>
