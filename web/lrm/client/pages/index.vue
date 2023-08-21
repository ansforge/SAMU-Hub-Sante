<template>
  <v-container fill-height>
    <v-layout align-center justify-center>
      <v-flex class="login-form text-center" style="max-width: 450px;">
        <div class="text-h4 my-10">
          <v-icon class="mr-2" x-large>
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
                :items="clientIds"
                :rules="[rules.required]"
              />
              <v-icon flat @click="swap">
                mdi-swap-vertical
              </v-icon>
              <v-combobox
                v-model="form.targetId"
                label="ID du système cible"
                :items="clientIds"
                :rules="[rules.required]"
              />
              <v-checkbox
                v-model="form.tester"
                label="Interface de test"
              />
              <v-alert
                v-if="alert.show"
                border="left"
                dense
                elevation="3"
                outlined
                :type="alert.type"
              >
                {{ alert.message }}
              </v-alert>
              <v-btn
                block
                type="submit"
                @click.prevent="login"
              >
                Se connecter
              </v-btn>
            </v-form>
          </v-card-text>
        </v-card>
      </v-flex>
    </v-layout>
  </v-container>
</template>

<script>
export default {
  name: 'Login',
  data () {
    return {
      alert: {
        show: false,
        type: 'error',
        message: ''
      },
      clientIds: [
        'fr.health.samuA',
        'fr.health.samuB',
        'fr.fire.nexsis.sdisZ'
      ],
      form: {
        clientId: 'fr.health.samuA',
        targetId: 'fr.health.samuB',
        tester: false
      },
      rules: {
        required: v => !!v || 'This field is required'
      }
    }
  },
  head () {
    return { title: 'Connexion' }
  },
  methods: {
    async login () {
      if (!this.$refs.form.validate()) { return }
      const loggedInUser = await this.$store.dispatch('logInUser', this.form)
      await this.$router.push(loggedInUser.tester ? '/test' : '/demo')
    },
    swap () {
      const clientId = this.form.clientId
      if (this.clientIds.includes(this.form.targetId)) {
        this.form.clientId = this.form.targetId
      } else {
        // Can only connect as a clientId in the authorised clientIds
        this.form.clientId = null
      }
      this.form.targetId = clientId
    }
  }
}
</script>
