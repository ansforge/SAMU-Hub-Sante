<template>
  <v-row justify="center">
    <v-col cols="12" sm="7">
      <v-card class="main-card" style="height: 86vh;">
        <v-card-title class="headline pb-">
          Cas de test <span class="font-weight-bold">&nbsp;{{ testCase.label }} </span>
        </v-card-title>
        <v-card-text class="main-card-content">
          <template v-for="(message, index) in selectedTypeCaseMessages">
            <div :key="'wrapper'+index" class="d-flex flex-column flex-wrap pb-3 pt-3">
              <ReceivedMessage
                v-bind="message"
                :key="message.time"
                class="message mb-4"
                :class="{ stale: message.stale, validated: message.validated }"
              />
              <v-btn v-if="message.validated" :key="'validated-label'+index" color="success" style="pointer-events: none;">
                <v-icon v-if="message.validated" :key="'icon'+index" class="validated-icon">
                  mdi-check
                </v-icon>
                Validé pour le pas {{ message.validatedStep+1 }}. {{ testCase.steps[message.validatedStep].label }}
              </v-btn>
            </div>
          </template>
        </v-card-text>
        <v-card-actions>
          <v-stepper v-model="currentStep" class="stepper">
            <v-stepper-header>
              <template v-for="(step, index) in testCase.steps">
                <v-stepper-step :key="index" :complete="index < currentStep-1" :step="index+1">
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
      <v-card class="main-card" style="height: 86vh;">
        <template v-if="currentStep-1 < testCase.steps.length">
          <v-card-title>
            {{ testCase.steps[currentStep-1]?.type === 'receive' ? 'Valeurs attendus dans le message' : 'Valeurs attendus dans l\'acquittement' }}
          </v-card-title>
          <v-card-text class="main-card-content">
            <p v-if="getAwaitedValues(testCase.steps[currentStep-1]) === null">
              En attente de la réception de l'ID de distribution...
            </p>
            <json-viewer
              v-else
              :value="getAwaitedValues(testCase.steps[currentStep-1])"
              :expand-depth="10"
              :copyable="{copyText: 'Copier', copiedText: 'Copié !', timeout: 1000}"
              expanded
              theme="json-theme"
            />
          </v-card-text>
          <v-card-actions v-if="testCase.steps[currentStep-1]?.type === 'send'">
            <v-btn color="primary" @click="submitMessage(testCase.steps[currentStep-1].json)">
              Envoyer
            </v-btn>
          </v-card-actions>
        </template>
        <template v-else>
          <v-card-title>
            Fin du cas de test
          </v-card-title>
          <v-card-text class="main-card-content">
            <v-card-text>
              <p>Le cas de test est terminé avec succés</p>
            </v-card-text>
          </v-card-text>
        </template>
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
      currentStep: 1,
      distributionIdOfSentMessage: null,
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
      return this.showableMessages
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
  watch: {
    selectedTypeCaseMessages: {
      handler (newMessages) {
        if (newMessages.length > 0) {
          const lastMessage = newMessages[0]
          if (!lastMessage.isOut) {
            if (this.checkMessage(lastMessage)) {
              this.validateMessage(this.selectedTypeCaseMessages.indexOf(lastMessage), true)
            }
          }
        }
      },
      deep: true
    }
  },
  created () {
    this.resetEverything()
    this.testCase = this.$route.params.testCase
    this.selectRandomValuesForTestCase()
  },
  mounted () {
    this.loadJsonSteps()
  },
  methods: {
    resetEverything () {
      this.currentStep = 1
    },
    selectRandomValuesForTestCase () {
      this.testCase.steps.forEach((step) => {
        if (step.type === 'receive') {
          step.message.requiredValues = this.selectRandomValuesForStep(step.message)
        }
      })
    },
    loadJsonSteps () {
      this.testCase.steps.map(async (step) => {
        const response = await fetch('/examples/' + step.message.file)
        const json = await response.json()
        this.$set(step, 'json', json)
      })
    },
    validateMessage (index, ack) {
      this.selectedTypeCaseMessages.forEach((message, i) => {
        if (i === index) {
          message.validatedStep = this.currentStep - 1
          message.validated = true
          if (ack) {
            if (this.getMessageType(message) !== 'ack' && message.routingKey.startsWith(this.user.clientId)) {
              const msg = this.buildAck(message.body.distributionID)
              this.sendMessage(msg)
            }
          }
        } else if (!message.validated) {
          message.stale = true
        }
      })
      this.nextStep()
    },
    nextStep () {
      this.currentStep++
      if (this.testCase.steps[this.currentStep - 1]?.type === 'send') {
        this.submitMessage(this.testCase.steps[this.currentStep - 1].json)
      }
    },
    submitMessage (message) {
      const builtMessage = this.buildMessage(message)
      this.distributionIdOfSentMessage = builtMessage.distributionID
      this.sendMessage(builtMessage)
    },
    getAwaitedValues (step) {
      if (step.type === 'receive') {
        const requiredValuesObject = {}
        step.message.requiredValues.forEach((entry) => {
          requiredValuesObject[entry.path] = entry.value
        })
        return requiredValuesObject
      } else {
        return this.getAwaitedReferenceDistributionIdJson()
      }
    },
    getAwaitedReferenceDistributionIdJson () {
      const json = {
        reference: {
          distributionID: this.distributionIdOfSentMessage
        }
      }
      return json
    },
    flattenObject (ob) {
      const toReturn = {}

      for (const i in ob) {
        if (!Object.prototype.hasOwnProperty.call(ob, i)) {
          continue
        }
        if ((typeof ob[i]) === 'object' && ob[i] !== null) {
          const flatObject = this.flattenObject(ob[i])
          for (const x in flatObject) {
            if (!Object.prototype.hasOwnProperty.call(flatObject, x)) {
              continue
            }
            toReturn[i + '.' + x] = flatObject[x]
          }
        } else {
          toReturn[i] = ob[i]
        }
      }
      return toReturn
    },
    selectRandomValuesForStep (step) {
      const selectedValues = []
      step.requiredValues.forEach((entry, index) => {
        selectedValues[index] = {
          path: entry.path,
          value: entry.value[Math.floor(Math.random() * entry.value.length)]
        }
      })
      return selectedValues
    },
    checkMessage (message) {
      if (this.testCase.steps[this.currentStep - 1].type === 'receive') {
        return this.checkMessageContainsAllRequiredValues(message, this.testCase.steps[this.currentStep - 1].message.requiredValues)
      } else {
        return this.checkAcknowledgementContainsReferenceDistributionId(message, this.getAwaitedReferenceDistributionIdJson())
      }
    },
    checkAcknowledgementContainsReferenceDistributionId (message, requiredValues) {
      const flattenedMessage = this.flattenObject(message)
      const flattenedRequiredValues = this.flattenObject(requiredValues)
      for (const requiredProp in flattenedRequiredValues) {
        let propFound = false
        for (const messageProp in flattenedMessage) {
          if (messageProp.endsWith(requiredProp)) {
            if (flattenedMessage[messageProp] !== flattenedRequiredValues[requiredProp]) {
              return false
            } else {
              propFound = true
              break
            }
          }
        }
        if (!propFound) {
          return false
        }
      }
      return true
    },
    checkMessageContainsAllRequiredValues (message, requiredValues) {
      const jp = require('jsonpath')
      return requiredValues.every(function (element) {
        const result = jp.query(message.body.content[0].jsonContent.embeddedJsonContent.message, element.path)
        if (result.length === 0 || !result.includes(element.value)) {
          return false
        } else {
          return true
        }
      })
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

.main-card-content{
  overflow-y: auto;
  flex-grow: 1;
}
</style>
