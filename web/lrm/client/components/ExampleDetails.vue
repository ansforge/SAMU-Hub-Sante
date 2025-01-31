<template>
  <v-card class="mx-auto" :border="true" tile elevation="4">
    <v-list-item lines="two">
      <template #prepend>
        <v-avatar size="64" color="grey">
          <v-icon color="rgba(0,0,0,.54)" size="38">
            {{ icon }}
          </v-icon>
        </v-avatar>
      </template>
      <v-list-item-title class="text-h5 mb-1">
        {{ name }}
      </v-list-item-title>
      <v-list-item-subtitle>{{ context }}</v-list-item-subtitle>

      <v-spacer />
      <template #append>
        <v-btn
          icon
          variant="text"
          :href="
            REPOSITORY_URL +
            $store.selectedVhost.modelVersion +
            '/src/main/resources/sample/examples/' +
            file
          "
          target="_blank"
        >
          <v-icon color="primary"> mdi-open-in-new </v-icon>
        </v-btn>
      </template>
    </v-list-item>
    <v-card-text class="pt-0">
      <div v-if="caller" class="py-1">
        <v-icon>mdi-phone</v-icon>
        {{ caller }}
      </div>
      <div v-if="environment" class="py-1">
        <v-icon>mdi-map-marker</v-icon>
        {{ environment }}
      </div>
      <div v-if="victims" class="py-1">
        <v-icon>mdi-account-multiple</v-icon>
        {{ victims }}
      </div>
      <div v-if="victim" class="py-1">
        <v-icon>mdi-account-injury</v-icon>
        {{ victim }}
      </div>
      <div v-if="medicalSituation" class="py-1">
        <v-icon>mdi-heart-pulse</v-icon>
        {{ medicalSituation }}
      </div>
    </v-card-text>
  </v-card>
</template>

<script>
  import { REPOSITORY_URL } from '@/constants';

  export default {
    props: {
      file: {
        type: String,
        required: true,
      },
      icon: {
        type: String,
        required: true,
      },
      name: {
        type: String,
        required: true,
      },
      caller: {
        type: String,
        required: true,
      },
      context: {
        type: String,
        required: true,
      },
      environment: {
        type: String,
        required: true,
      },
      victims: {
        type: String,
        required: true,
      },
      victim: {
        type: String,
        required: true,
      },
      medicalSituation: {
        type: String,
        required: true,
      },
    },
    data() {
      return {
        showDetails: false,
        REPOSITORY_URL,
      };
    },
  };
</script>

<style scoped>
  i {
  color: rgb(100 100 100);
}
</style>
