<template>
  <v-row justify="center">
    <v-col cols="12" sm="7">
      <v-card class="main-card" style="height: 86vh; overflow-y: auto;">
        <v-card-title class="headline pb-">
          Cas de test <span class="font-weight-bold">&nbsp;{{ testCase.label }} </span>
        </v-card-title>
        <v-card-text>
          <template v-for="(message, index) in selectedTypeCaseMessages">
            <ReceivedMessage
              v-bind="message"
              :key="message.time"
              class="message mb-4"
              :class="{ stale: message.stale, validated: message.validated }"
            />
            <v-btn v-if="!(message.validated || message.stale)" :key="'button'+index" color="primary" @click="validateMessage(index)">
              Valider
            </v-btn>
            <span v-if="message.validated" :key="'label'+index" class="step-label">
              {{ testCase.steps[message.validatedStep].label }}
            </span>
            <v-icon v-if="message.validated" :key="'icon'+index" class="validated-icon">
              mdi-check
            </v-icon>
          </template>
        </v-card-text>
        <v-card-actions>
          <v-stepper class="stepper">
            <v-stepper-header>
              <template v-for="(step, index) in testCase.steps">
                <v-stepper-step :key="index" :complete="index < currentStep" :step="index + 1">
                  {{ step.label }}
                </v-stepper-step>
                <v-divider v-if="index < testCase.steps.length - 1" :key="'divider' + index" />
              </template>
            </v-stepper-header>
          </v-stepper>
        </v-card-actions>
      </v-card>
    </v-col>
    <v-col cols="12" sm="5">
      <v-card style="height: 86vh; overflow-y: auto;">
        <v-card-title>
          {{ testCase.steps[currentStep]?.type === 'receive' ? 'Message attendu' : 'Message envoye' }}
        </v-card-title>
        <v-card-text>
          <v-card-text>
            <pre>{{ testCase.steps[currentStep]?.json }}</pre>
          </v-card-text>
        </v-card-text>
      </v-card>
    </v-col>
  </v-row>
</template>

<script>

import { mapGetters } from 'vuex'
import mixinMessage from '~/plugins/mixinMessage'

export default {
  name: 'Testcase',
  mixins: [mixinMessage],
  data () {
    return {
      testCase: null,
      currentStep: 0,
      selectedMessageType: 'message',
      selectedClientId: null,
      selectedCaseIds: [],
      queueTypes: [
        {
          name: 'Message',
          type: 'message',
          icon: 'mdi-message'
        },
        {
          name: 'Ack',
          type: 'ack',
          icon: 'mdi-check'
        },
        {
          name: 'Info',
          type: 'info',
          icon: 'mdi-information'
        }
      ]
    }
  },
  computed: {
    ...mapGetters(['messages', 'isAdvanced']),
    clientMessages () {
      return this.messages.filter(
        message =>
          (this.isOut(message.direction) &&
            message.body.senderID === this.user.clientId) ||
          (!this.isOut(message.direction) &&
            message.routingKey.startsWith(this.user.clientId))
      )
    },
    showableMessages () {
      return this.showSentMessages
        ? this.clientMessages
        : this.clientMessages.filter(message => !this.isOut(message.direction))
    },
    selectedTypeMessages () {
      return this.showableMessages.filter(
        message => this.getMessageType(message) === this.selectedMessageType
      )
    },
    selectedTypeCaseMessages () {
      if (this.selectedCaseIds.length === 0) {
        return this.selectedTypeMessages
      }
      return this.selectedTypeMessages.filter(message =>
        this.selectedCaseIds.includes(this.getCaseId(message))
      )
    },
    caseIds () {
      return [...new Set(this.selectedTypeMessages.map(this.getCaseId))]
    }
  },
  created () {
    this.testCase = this.$route.params.testCase
  },
  mounted () {
    this.loadJsonSteps()
  },
  methods: {
    loadJsonSteps () {
      this.testCase.steps.map(async (step) => {
        const response = await fetch('/examples/' + step.message.file)
        const json = await response.json()
        this.$set(step, 'json', json)
      })
    },
    validateMessage (index) {
      this.selectedTypeCaseMessages.forEach((message, i) => {
        if (i === index) {
          message.validatedStep = this.currentStep
          message.validated = true
        } else {
          message.stale = true
        }
      })
      this.currentStep++
    }
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

.message {
  position: relative;
}

.message .validated-icon {
  position: absolute;
  top: 0;
  right: 0;
  color: green;
}

.message.stale {
  opacity: 0.5;
}

.step-label {
  color: lightgreen;
  font-weight: bold;
}
</style>
