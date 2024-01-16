<template>
  <v-row justify="center">
    <v-col cols="12" sm="7">
      <v-card class="main-card" style="height: 86vh;">
        <v-card-title class="headline pb-0">
          Cas de test <span class="font-weight-bold">&nbsp;{{ testCase.label }} </span>
        </v-card-title>
        <v-card-actions class="pt-0" style="flex-direction: column;">
          <v-container class="pt-0" full-width>
            <v-stepper v-model="currentStep" class="stepper">
              <v-stepper-header>
                <template v-for="(step, index) in testCase.steps">
                  <v-col :key="'step' + index">
                    <v-stepper-step
                      :key="index"
                      style="cursor: pointer;"
                      :color="getStepColor(index)"
                      :complete="index < currentStep-1"
                      :step="index+1"
                      @click="currentlySelectedStep=index+1"
                    >
                      {{ step.label }}
                    </v-stepper-step>
                  </v-col>
                  <v-divider v-if="index < testCase.steps.length - 1" :key="'divider' + index" />
                </template>
              </v-stepper-header>
            </v-stepper>
          </v-container>
          <v-container>
            <span>
              {{ testCase.steps[currentlySelectedStep-1]?.description }}
            </span>
          </v-container>
        </v-card-actions>
        <v-card-text class="main-card-content">
          <v-container full-width>
            <v-row>
              <v-col class="small-message">
                <template v-for="(message, index) in selectedTypeCaseMessages">
                  <div v-if="message.relatedStep===currentlySelectedStep-1" :key="'wrapper'+index" class="d-flex flex-column flex-wrap pb-1 pt-1" @click="setSelectedMessage(message)">
                    <ReceivedMessage
                      :key="message.time"
                      :dense="true"
                      :validated-values-count="message?.validatedValues?.filter(value => value.valid).length"
                      :required-values-count="testCase.steps[message.relatedStep]?.type === 'send' ? testCase.steps[message.relatedStep]?.message?.requiredValues?.length : 1"
                      v-bind="message"
                      class="message mb-4"
                      :class="{ stale: message.stale, validated: message.validated, selected: selectedMessageIndex === index }"
                    />
                  </div>
                </template>
              </v-col>
              <v-col v-if="selectedMessage?.relatedStep===currentlySelectedStep-1" class="full-message">
                <!-- Details of selected message (last received or sent by default)-->
                <ReceivedMessage
                  v-if="selectedMessage"
                  :key="selectedMessage?.time"
                  :json-depth="10"
                  v-bind="selectedMessage"
                  class="message mb-4"
                  :class="{ stale: selectedMessage?.stale, validated: selectedMessage?.validated }"
                />
              </v-col>
            </v-row>
          </v-container>
        </v-card-text>
      </v-card>
    </v-col>
    <v-col cols="12" sm="5">
      <v-card class="main-card" style="height: 86vh; overflow-y:auto">
        <template v-if="currentStep-1 < testCase.steps.length">
          <span>
            <v-card-title>
              {{ testCase.steps[currentStep-1]?.type === 'send' ? 'Valeurs attendues dans le message' : 'Valeurs attendues dans l\'acquittement' }}
            </v-card-title>
            <v-card-text>
              <p v-if="getAwaitedValues(testCase.steps[currentStep-1]) === null">
                En attente de la réception de l'ID de distribution...
              </p>
              <v-list v-for="(requiredValue, name, index) in getAwaitedValues(testCase.steps[currentStep-1])" :key="'requiredValue' + index">
                <v-list-item-content>
                  <span style="display: flex; flex-direction: row; align-items: center;">
                    <pre class="values">{{ name }} : {{ requiredValue.value }}</pre>
                  </span>
                </v-list-item-content>
              </v-list>
              <v-list v-if="testCase.steps[currentStep-1]?.type === 'receive'">
                <!-- Generate an input for each requiredValue with the path used as label. User will enter a value for each requiredValue and then press a button to verify that all the values entered correspond to the values in the requiredValues-->
                <v-list-item v-for="(requiredValue, index) in testCase.steps[currentStep-1].message.requiredValues" :key="'requiredValue' + index">
                  <v-list-item-content>
                    <v-icon v-if="requiredValue.valid " style="flex:0" color="success">
                      mdi-check
                    </v-icon>
                    <v-icon v-else style="flex:0" color="error">
                      mdi-close
                    </v-icon>
                    <v-text-field
                      v-model="requiredValue.enteredValue"
                      :label="requiredValue.path"
                      :rules="[v => !!v || 'Valeur requise']"
                      required
                    />
                  </v-list-item-content>
                </v-list-item>
                <!-- Button that would execute the verification of value conformity -->
                <v-btn v-if="!testCase.steps[currentStep-1].message.validatedReceivedValues" color="primary" @click="validateEnteredValues(currentStep-1)">
                  Valider
                </v-btn>
              </v-list>
            </v-card-text>
          </span>
          <!-- Currently selected message's valid and invalid required values -->
          <span v-if="selectedMessage&&!isOut(selectedMessage.direction)&&selectedMessage?.validatedValues?.length>0">
            <v-card-title>
              {{ 'Valeurs reçues dans le message séléctionné' }}
            </v-card-title>
            <v-card-text>
              <v-list v-for="(validatedValue, index) in selectedMessage?.validatedValues" :key="'validatedValue' + index">
                <v-list-item-content class="d-flex flex-wrap">
                  <v-icon v-if="validatedValue.valid" style="flex:0" color="success">
                    mdi-check
                  </v-icon> <v-icon v-else style="flex:0" color="error">
                    mdi-close
                  </v-icon>
                  <pre class="values">{{ validatedValue?.value?.path }} : {{ validatedValue?.value?.value }} <span v-if="!validatedValue?.valid" class="wrong-received"> (Reçu: {{ validatedValue?.receivedValue || 'null' }}) </span></pre>
                </v-list-item-content>
              </v-list>
            </v-card-text>
          </span>
          <v-card-actions v-if="testCase.steps[currentStep-1]?.type === 'receive'">
            <v-btn color="primary" @click="submitMessage(testCase.steps[currentStep-1])">
              Re-envoyer le message
            </v-btn>
          </v-card-actions>
        </template>
        <template v-else>
          <v-card-title>
            Fin du cas de test
          </v-card-title>
          <v-card-text class="main-card-content">
            <v-card-text>
              <p>Le cas de test est terminé avec succès 🥳</p>
            </v-card-text>
          </v-card-text>
        </template>
      </v-card>
    </v-col>
  </v-row>
</template>

<script>

import { mapGetters } from 'vuex'
import Vue from 'vue'
import mixinMessage from '~/plugins/mixinMessage'

export default {
  name: 'Testcase',
  mixins: [mixinMessage],
  data () {
    return {
      currentCaseId: null,
      testCase: null,
      currentlySelectedStep: 1,
      currentStep: 1,
      selectedMessageIndex: 0,
      selectedMessageType: 'message',
      selectedClientId: null,
      selectedCaseIds: [],
      handledLength: 0,
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
    selectedMessage () {
      return this.selectedTypeCaseMessages[this.selectedMessageIndex]
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
    /**
     * Checks and potentially validates the last message if it is not an outgoing message
     */
    selectedTypeCaseMessages: {
      handler (newMessages) {
        // Reset selected message index whenever we receive a new message
        this.selectedMessageIndex = 0
        // We get difference between previously handled length and current length to only check the messages that were added
        if (this.currentStep <= this.testCase.steps.length && newMessages.length > 0) {
          // We iterate over the new messages, starting from the furthest one from the beginning of the array
          for (let i = (newMessages.length - this.handledLength - 1); i >= 0; i--) {
            const lastMessage = newMessages[i]
            // We set the relatedStep property to currentStep-1 unless it's an acknowledgement, in which case
            // we search for the related message and set the relatedStep property to the step at which the message was sent
            if (this.getMessageType(lastMessage) === 'ack' && this.isOut(lastMessage.direction)) {
              const relatedMessage = this.messages.find(message => message.body?.content[0]?.jsonContent?.embeddedJsonContent?.message?.messageId === lastMessage.body?.content[0]?.jsonContent?.embeddedJsonContent?.message?.reference?.distributionID)
              lastMessage.relatedStep = relatedMessage.relatedStep
            } else {
              lastMessage.relatedStep = this.currentStep - 1
            }
            if (!lastMessage.isOut) {
              if (this.checkMessage(lastMessage)) {
                const shouldStayOnStep = this.testCase.steps[this.currentStep - 1].type === 'receive' && !(this.testCase.steps[this.currentStep - 1].message.validatedAcknowledgement && this.testCase.steps[this.currentStep - 1].message.validatedReceivedValues)
                this.validateMessage(newMessages.indexOf(lastMessage), true, shouldStayOnStep)
              }
            }
          }
          this.handledLength = newMessages.length
        }
      },
      deep: true
    }
  },
  created () {
    this.currentCaseId = null
    this.currentStep = 1
    this.testCase = this.$route.params.testCase
    this.selectRandomValuesForTestCase()
  },
  mounted () {
    this.initialize()
  },
  methods: {
    async initialize () {
      await this.loadJsonSteps()

      if (this.testCase.steps[this.currentStep - 1]?.type === 'receive') {
        this.submitMessage(this.testCase.steps[this.currentStep - 1])
      }
    },
    /**
     * Selects one value randomly from the list of possible values
     * for each required value in the test case
     */
    selectRandomValuesForTestCase () {
      this.testCase.steps.forEach((step) => {
        step.message.requiredValues = this.selectRandomValuesForStep(step.message)
      })
    },
    /**
     * Loads the related JSON message for the test case steps.
     * Should only be necesary for 'receive' steps, as 'send' steps
     * only expect specific key:value pairs in the message.
     */
    async loadJsonSteps () {
      for (const step of this.testCase.steps) {
        if (step.type === 'receive') {
          const response = await fetch('examples/' + step.message.file)
          const json = await response.json()
          this.$set(step, 'json', json)
        }
      }
    },
    /**
     * Marks the message as validated, recording the test case step at which it was validated
     * and sends an acknowledgement if the message is not an acknowledgement itself and if
     * it was indicated in the parameters.
     * @param {*} index Index of the message in the list of messages
     * @param {*} ack Indicates if an acknowledgement should be sent
     * @param {*} stayOnStep Indicates if the test case should stay on the current step or move to the next one
     */
    validateMessage (index, ack, stayOnStep = false) {
      this.selectedTypeCaseMessages.forEach((message, i) => {
        if (i === index) {
          // If we don't have currentCaseId set, we set it to the value of the case Id the sent message
          if (!this.currentCaseId) {
            this.currentCaseId = this.getCaseId(message.body)
          }
          message.validatedStep = this.currentStep - 1
          message.validated = true
          if (ack) {
            if (this.getMessageType(message) !== 'ack' && message.routingKey.startsWith(this.user.clientId)) {
              const msg = this.buildAck(message.body.distributionID)
              this.sendMessage(msg)
            }
          }
        // We mark every other message currently present in the message array as stale, making them unvalidatable
        } else if (!message.validated && message.relatedStep === this.currentStep - 1) {
          message.stale = true
        }
      })
      if (!stayOnStep) {
        this.nextStep()
      }
    },
    /**
     * Increments current test case step and sends a message if the step is a 'receive' step after incrementing
     */
    nextStep () {
      this.currentStep++
      this.currentlySelectedStep = this.currentStep
      if (this.testCase.steps[this.currentStep - 1]?.type === 'receive') {
        this.submitMessage(this.testCase.steps[this.currentStep - 1])
      }
    },
    /**
     * Builds a message from the JSON and sends it
     * @param {*} message JSON message to send
     */
    submitMessage (step) {
      let message = step.json
      // Use the required values to replace the corresponding values in the message
      message = this.replaceValues(message, step.message.requiredValues)
      // Replace the createCase.caseId or emsi.EVENT.ID values with the current case ID if current case ID is already defined
      if (this.currentCaseId) {
        if (message.createCase) {
          message = this.replaceValues(message, [{ path: '$.createCase.caseId', value: this.currentCaseId }])
        } else if (message.emsi) {
          message = this.replaceValues(message, [{ path: '$.emsi.EVENT.MAIN_EVENT_ID', value: this.currentCaseId }])
        }
      }
      const builtMessage = this.buildMessage(message)
      this.testCase.steps[this.currentStep - 1].message.awaitedReferenceDistributionID = builtMessage.distributionID
      this.sendMessage(builtMessage)
      // If we sent a message but don't currently have current case Id set, we set it to the value of the case Id in the sent message
      if (!this.currentCaseId) {
        this.currentCaseId = this.getCaseId(builtMessage)
      }
    },
    /**
     * Gets the case id from the message, whether it's an rc or emsi message
     */
    getCaseId (message) {
      if (message.content[0].jsonContent.embeddedJsonContent.message.createCase) {
        return message.content[0].jsonContent.embeddedJsonContent.message.createCase.caseId
      } else if (message.content[0].jsonContent.embeddedJsonContent.message.emsi) {
        return message.content[0].jsonContent.embeddedJsonContent.message.emsi.EVENT.MAIN_EVENT_ID
      }
    },
    /**
     * Replaces values in a message using path:value pairs
     */
    replaceValues (message, requiredValues) {
      const jp = require('jsonpath')
      requiredValues.forEach((entry) => {
        jp.value(message, entry.path, entry.value)
      })
      return message
    },
    /**
     * Returns the required values for a given step
     * @param {*} step Step for which to return the required values
     */
    getAwaitedValues (step) {
      if (step.type === 'send') {
        const requiredValuesObject = {}
        step.message.requiredValues.forEach((entry) => {
          requiredValuesObject[entry.path] = {
            value: entry.value,
            valid: entry.valid
          }
        })
        return requiredValuesObject
      } else {
        return this.getAwaitedReferenceDistributionObject(step)
      }
    },
    /**
     * Verifies that the values entered by the user are the same as the required values (for 'receive' type steps)
     * These values are stored in the 'requiredValues' array of the step, and replace the relevant values in the JSON when the message is sent
     * @param {*} step
     */
    validateEnteredValues (step) {
      const requiredValues = this.testCase.steps[step].message.requiredValues
      let valid = true
      requiredValues.forEach((entry) => {
        if (entry.enteredValue !== String(entry.value)) {
          valid = false
          entry.valid = false
        } else {
          entry.valid = true
        }
      })
      if (valid) {
        this.testCase.steps[step].message.validatedReceivedValues = true
        // Also go to next step if the acknowledgement has already been validated
        if (this.testCase.steps[step].message.validatedAcknowledgement) {
          this.nextStep()
        }
      }
      Vue.set(this, 'testCase', { ...this.testCase })
    },
    /**
     * Returns the JSON array containing an object with the same structure as 'requiredValues'
     * for 'send' steps, used for validation during 'receive' steps
     */
    getAwaitedReferenceDistributionIdJson (step) {
      const json = [
        {
          path: '$.reference.distributionID',
          value: step?.message?.awaitedReferenceDistributionID
        }
      ]
      return json
    },
    /**
     * Returns the JSON object containing a property by the name of the path to reference distribution ID in the
     * acknowledgement message json and its expected value as property 'value' for a specific step, which is the verified value
     * during 'receive' steps
     * @param {*} step
     */
    getAwaitedReferenceDistributionObject (step) {
      const json = {
        '$.reference.distributionID': {
          value: step?.message?.awaitedReferenceDistributionID
        }
      }
      return json
    },
    /**
     * Utility function to flatten a JSON object
     * @param {*} ob JSON object to flatten
     */
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
    /**
     * Select a random value for each required value in a step
     * @param {*} step Step for which to select random values
     */
    selectRandomValuesForStep (step) {
      const selectedValues = []
      step.requiredValues.forEach((entry, index) => {
        selectedValues[index] = {
          path: entry.path,
          value: Array.isArray(entry.value) ? entry.value[Math.floor(Math.random() * entry.value.length)] : entry.value
        }
      })
      return selectedValues
    },
    /**
     * Checks if a message contains all required values
     * @param {*} message Message to check
     */
    checkMessage (message) {
      if (this.testCase.steps[this.currentStep - 1].type === 'send') {
        return this.checkMessageContainsAllRequiredValues(message, this.testCase.steps[this.currentStep - 1].message.requiredValues)
      } else if (!this.testCase.steps[this.currentStep - 1].message.validatedAcknowledgement) {
        message.validatedAcknowledgement = this.checkMessageContainsAllRequiredValues(message, this.getAwaitedReferenceDistributionIdJson(this.testCase.steps[this.currentStep - 1]))
        Vue.set(this.testCase.steps[this.currentStep - 1].message, 'validatedAcknowledgement', message.validatedAcknowledgement)
        if (message.validatedAcknowledgement) {
          this.validateMessage(this.selectedTypeCaseMessages.indexOf(message), false, true)
        }
        return this.testCase.steps[this.currentStep - 1].message.validatedAcknowledgement && this.testCase.steps[this.currentStep - 1].message.validatedReceivedValues
      }
    },
    /**
     * Checks if an acknowledgement contains the indicated reference distribution ID
     * @param {*} message Acknowledgement to check
     * @param {*} requiredValues Reference distribution ID to check
     */
    checkAcknowledgementContainsReferenceDistributionId (message, requiredValues) {
      const flattenedMessage = this.flattenObject(message)
      const flattenedRequiredValues = this.flattenObject(requiredValues)
      message.validatedValues = []
      for (const requiredProp in flattenedRequiredValues) {
        let propFound = false
        for (const messageProp in flattenedMessage) {
          if (messageProp.endsWith(requiredProp)) {
            if (flattenedMessage[messageProp] !== flattenedRequiredValues[requiredProp]) {
              message.validatedValues.push({ valid: false, value: requiredValues })
              return false
            } else {
              propFound = true
              break
            }
          }
        }
        if (!propFound) {
          message.validatedValues.push({ valid: false, value: requiredValues })
          return false
        }
      }
      message.validatedValues.push({ valid: true, value: requiredValues })
      return true
    },
    /**
     * Checks if a message contains all required values
     * @param {*} message Message to check
     * @param {*} requiredValues Required values to check
     */
    checkMessageContainsAllRequiredValues (message, requiredValues) {
      const jp = require('jsonpath')
      let valid = true
      const validatedValues = []

      requiredValues.forEach(function (element) {
        const result = jp.query(message.body.content[0].jsonContent.embeddedJsonContent.message, element.path)
        if (result.length === 0 || !result.includes(element.value)) {
          valid = false
          element.valid = false
          validatedValues.push({ valid: false, value: element, receivedValue: result[0] })
        } else {
          validatedValues.push({ valid: true, value: element })
          element.valid = true
        }
      })
      message.validatedValues = JSON.parse(JSON.stringify(validatedValues))
      Vue.set(this, 'message', message)
      return valid
    },
    getStepColor (index) {
      if (index === this.currentlySelectedStep - 1) {
        return 'primary'
      } else if (index === this.currentStep - 1) {
        return '#CFE2F6'
      } else if (index < this.currentStep - 1) {
        return 'green'
      } else {
        return 'grey'
      }
    },
    setSelectedMessage (message) {
      this.selectedMessageIndex = this.selectedTypeCaseMessages.indexOf(message)
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

.main-card-content{
  overflow-y: auto;
  flex-grow: 1;
  width: 100%;
}

.v-stepper__step--inactive {
  pointer-events: none;
}

.wrong-received {
  color: red;
  font-weight: bold;
}

.small-message {
  max-width: fit-content;
}

pre.values {
  flex: 1;
  text-wrap: wrap;
  line-break: anywhere;
}

.v-stepper__header {
  height: auto;
}
</style>
