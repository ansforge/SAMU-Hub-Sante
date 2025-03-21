<template>
  <v-container fill-height>
    <v-layout class="d-flex justify-center">
      <v-col class="login-form text-center" style="max-width: 500px">
        <v-card variant="outlined" class="pa-4">
          <v-card-title class="d-flex align-center justify-center my-4">
            <v-img :max-width="66" src="~/assets/images/icons/SMUR.png"></v-img>

            <span class="mx-4 font-weight-bold">49 - Angers</span>
          </v-card-title>
          <v-card-text>
            <v-form ref="form" action="#" @submit.prevent="">
              <v-text-field
                v-model="password"
                :append-icon="showPassword ? 'mdi-eye' : 'mdi-eye-off'"
                :type="showPassword ? 'text' : 'password'"
                label="Mot de passe"
                :rules="[rules.required]"
                @click:append="showPassword = !showPassword"
                @keyup.enter="login('/carto')"
              />

              <v-alert
                v-if="alert.show"
                border="start"
                density="compact"
                elevation="3"
                variant="outlined"
                :type="alert.type"
                class="mb-3"
              >
                {{ alert.message }}
              </v-alert>

              <v-btn
                data-cy="carto-login-button"
                color="primary"
                class="mb-5"
                block
                type="submit"
                @click="login('/carto')"
              >
                Connexion
              </v-btn>
            </v-form>
          </v-card-text>
        </v-card>
      </v-col>
    </v-layout>
  </v-container>
</template>

<script setup>
// eslint-disable-next-line no-undef
useHead({
  title: 'Connexion - Hub Santé Géoloc',
});
</script>

<script>
import { navigateTo } from 'nuxt/app';
import { useMainStore } from '~/store';
import { useAuthStore } from '~/store/auth';

export default {
  name: 'Login',

  data() {
    return {
      store: useMainStore(),
      alert: {
        show: false,
        type: 'error',
        message: '',
      },
      password: '',
      showPassword: false,
      rules: {
        required: (v) => !!v || 'Ce champ est requis',
      },
    };
  },
  methods: {
    async login(target) {
      try {
        if (!(await this.$refs.form.validate()).valid) {
          return;
        }
        const authStore = useAuthStore();
        await authStore.login(this.password);
        navigateTo(target);
      } catch (error) {
        this.alert = {
          show: true,
          type: 'error',
          message: 'Mot de passe incorrect.',
        };
      }
      // eslint-disable-next-line no-undef
    },
  },
};
</script>
