<template>
  <v-container fill-height>
    <v-layout class="d-flex justify-center">
      <v-col class="login-form text-center " style="max-width: 450px;">
        <div class="text-h4 my-10">
          <v-icon class="mr-2" size="small" color="rgb(100,100,100)">
            mdi-heart-pulse
          </v-icon>
          Hub Santé LRM
        </div>
        <v-card class="my-5">
          <v-card-text>
            <div class="subheading">
              Choisissez le lien que vous voulez tester
            </div>
            <v-form ref="form">
              <v-select
                v-model="form.clientId"
                label="ID du système utilisé"
                :items="[...clientIds.keys()]"
                :rules="[rules.required]"
              />
              <v-icon class="mb-4" @click="swap">
                mdi-swap-vertical
              </v-icon>
              <v-combobox
                v-model="form.targetId"
                label="ID du système cible"
                :items="targetClientIds"
                :rules="[rules.required, rules.testTargetId]"
              />
              <v-alert
                v-if="alert.show"
                border="start"
                density="compact"
                elevation="3"
                variant="outlined"
                :type="alert.type"
              >
                {{ alert.message }}
              </v-alert>
              <v-btn
                color="primary"
                class="mb-5"
                block
                type="submit"
                @click.prevent="login('/demo')"
              >
                LRM de test
              </v-btn>
              <v-btn
                color="primary"
                block
                type="submit"
                @click.prevent="login('/test')"
              >
                Recette guidée
              </v-btn>
            </v-form>
          </v-card-text>
        </v-card>
      </v-col>
    </v-layout>
  </v-container>
</template>

<script setup>
import { useMainStore } from '~/store'

useHead({
  title: 'Connexion - Hub Santé'
})
</script>

<script>

export default {

  name: 'Login',

  data () {
    return {
      store: useMainStore(),
      alert: {
        show: false,
        type: 'error',
        message: ''
      },
      clientIds: Object.keys(this.$config.public.clientMap).length === 0 ? new Map() : new Map(this.$config.public.clientMap),
      form: {
        clientId: 'fr.health.samuA',
        targetId: 'fr.health.samuC',
        tester: false
      },
      rules: {
        required: v => !!v || 'This field is required',
        testTargetId: (v) => {
          if (this.form.clientId.startsWith('fr.health.test') && !this.targetClientIds.includes(v)) {
            return "Tests are only allowed on editor's systems"
          }
          return true
        }
      }
    }
  },
  computed: {
    targetClientIds () {
      return this.clientIds.get(this.form.clientId)
    }
  },
  methods: {
    async login (target) {
      if (!this.$refs.form.validate()) { return }
      await this.store.logInUser(this.form)
      await navigateTo(target)
    },
    swap () {
      const clientId = this.form.clientId
      if (this.clientIds.has(this.form.targetId)) {
        this.form.clientId = this.form.targetId
      } else {
        // Can only connect as a clientId in the authorized clientIds
        this.form.clientId = null
      }
      this.form.targetId = clientId
    }
  }
}
</script>
