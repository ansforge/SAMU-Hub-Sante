<template>
  <v-row justify="center">
    <v-col cols="12" sm="7">
      <v-card class="main-card" style="height: 86vh; overflow-y: auto;">
        <v-card-title class="headline pb-">
          Cas de test <span class="font-weight-bold">&nbsp;{{ testCase.label }} </span>
        </v-card-title>
        <v-card-text>
          Message log goes here
        </v-card-text>
        <v-card-actions>
          <v-stepper class="stepper">
            <v-stepper-header>
              <template v-for="(step, index) in testCase.steps">
                <v-stepper-step
                  :key="index"
                  :complete="index < currentStep"
                  :step="index + 1"
                >
                  {{ step.label }}
                </v-stepper-step>
                <v-divider v-if="index < testCase.steps.length - 1" :key="'divider'+index" />
              </template>
            </v-stepper-header>
          </v-stepper>
        </v-card-actions>
      </v-card>
    </v-col>
    <v-col cols="12" sm="5">
      <v-card style="height: 86vh; overflow-y: auto;">
        <v-card-title>
          {{ testCase.steps[currentStep].type === 'receive' ? 'Message attendu' : 'Message envoye' }}
        </v-card-title>
        <v-card-text>
          <exampleDetails v-bind="testCase.steps[currentStep].message" />
        </v-card-text>
      </v-card>
    </v-col>
  </v-row>
</template>

<script>
export default {
  name: 'Testcase',
  data () {
    return {
      testCase: null,
      currentStep: 0
    }
  },
  created () {
    this.testCase = this.$route.params.testCase
  }
}
</script>

<style scoped>
h1 {
  color: blue;
}

p {
  font-size: 18px;
}

div.stepper.v-stepper {
  box-shadow: none;
  justify-content: space-between;
  width: 100%;
}

.main-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
</style>
