<template>
  <v-row justify="center">
    <v-col cols="12" sm="7">
      <v-card class="main-card" style="height: 86vh;">
        <v-card-title class="d-flex text-h5 pb-0">
          Cas de test <span class="font-weight-bold">&nbsp;{{ testCase?.label }} </span>
          <vhost-selector class="mr-5" />
          <v-btn v-if="testCase?.steps[currentStep-1]?.type === 'receive'" color="primary" @click="submitMessage(testCase?.steps[currentStep-1])">
            Re-envoyer le message
          </v-btn>
        </v-card-title>
        <v-card-actions class="pt-0" style="flex-direction: column;">
          <v-container class="pt-0" full-width>
            <v-stepper v-model="currentStep" class="stepper">
              <v-stepper-header>
                <template v-for="(step, index) in testCase?.steps" :key="'step' + index">
                  <v-col>
                    <v-stepper-item
                      :key="index"
                      :value="index+1"
                      editable
                      style="cursor: pointer;"
                      :color="getStepColor(index)"
                      :step="index+1"
                      edit-icon="mdi-circle"
                      @click="goToStep(index+1)"
                    >
                      {{ step.label }}
                    </v-stepper-item>
                  </v-col>
                  <v-divider v-if="index < testCase?.steps.length - 1" :key="'divider' + index" />
                </template>
              </v-stepper-header>
            </v-stepper>
          </v-container>
          <v-container>
            <span>
              {{ testCase?.steps[currentlySelectedStep-1]?.description }}
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
                      :required-values-count="testCase?.steps[message.relatedStep]?.type === 'send' ? testCase?.steps[message.relatedStep]?.requiredValues?.length : 1"
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
        <template v-if="currentStep-1 < testCase?.steps.length">
          <span>
            <v-card-title>
              {{ testCase?.steps[currentStep-1]?.type === 'send' ? 'Valeurs attendues dans le message' : 'Valeurs attendues dans l\'acquittement' }}
            </v-card-title>
            <v-card-text>
              <p v-if="getAwaitedValues(testCase?.steps[currentStep-1]) === null">
                En attente de la réception de l'ID de distribution...
              </p>
              <v-list>
                <v-list-item v-for="(requiredValue, name, index) in getAwaitedValues(testCase?.steps[currentStep-1])" :key="'requiredValue' + index">
                  <span style="display: flex; flex-direction: row; align-items: center;">
                    <pre class="values"><b>{{ name }}:</b><br> {{ requiredValue.value }}</pre>
                  </span>
                </v-list-item>
              </v-list>
            </v-card-text>

            <v-card-title>
              Valeurs reçues dans le message
            </v-card-title>
            <v-card-text v-if="testCase?.steps[currentStep-1]?.type === 'receive'">
              <v-list>
                <!-- Generate a list of paths:values from required values and add three buttons for each entry, letting user indicate whether the value they received is correct, 'somewhat' correct or incorrect-->
                <v-list-item v-for="(requiredValue, index) in testCase?.steps[currentStep-1].requiredValues" :key="'requiredValue' + index" class="received-values-list">
                  <span class="d-flex flex-row align-center">
                    <span class="confirmation-buttons d-flex flex-row">
                      <!-- Grey out buttons that do not correspond to the validation state if the value has already been validated -->
                      <v-btn density="compact" icon="mdi-check" :color="(requiredValue.valid === 'valid' || requiredValue.valid === undefined) ? 'success' : 'grey' " @click="requiredValue.valid = 'valid'" />
                      <v-btn density="compact" icon="mdi-tilde" :color="(requiredValue.valid === 'approximate' || requiredValue.valid === undefined) ? 'warning' : 'grey' " @click="requiredValue.valid = 'approximate'" />
                      <v-btn density="compact" icon="mdi-close" :color="(requiredValue.valid === 'invalid' || requiredValue.valid === undefined) ? 'error' : 'grey' " @click="requiredValue.valid = 'invalid'" />
                    </span>
                    <span>
                      <pre class="values" :style="{color: requiredValue.valid === 'valid' ? 'green' : requiredValue.valid === 'approximate' ? 'orange' : requiredValue.valid === 'invalid' ? 'red' : 'black'}"><b>{{ requiredValue.path.join('.') }}:</b> <br>{{ requiredValue.value }}</pre>
                    </span>

                  </span>
                </v-list-item>
              </v-list>
            </v-card-text>
            <v-card-text>
              <!-- Button that validates the step and goes to the next -->
              <v-btn v-if="!testCase?.steps[currentStep-1].validatedReceivedValues" color="primary" @click="validateStep(currentStep-1)">
                Passer à l'étape suivante
              </v-btn>
            </v-card-text>
          </span>
          <!-- Currently selected message's valid and invalid required values -->
          <span v-if="selectedMessage&&!isOut(selectedMessage.direction)&&selectedMessage?.validatedValues?.length>0">
            <v-card-title>
              {{ 'Valeurs reçues dans le message séléctionné' }}
            </v-card-title>
            <v-card-text>
              <v-list>
                <v-list-item v-for="(validatedValue, index) in selectedMessage?.validatedValues" :key="'validatedValue' + index" class="d-flex flex-row flex-wrap">
                  <span class="d-flex flex-row flex-wrap">
                    <v-icon v-if="validatedValue.valid" style="flex:0" color="success">
                      mdi-check
                    </v-icon> <v-icon v-else style="flex:0" color="error">
                      mdi-close
                    </v-icon>
                    <pre class="values"><b>{{ validatedValue?.value?.path?.join('.') }}:</b> <br>{{ validatedValue?.value?.value }}<br><span v-if="!validatedValue?.valid" class="wrong-received">(Reçu: {{ validatedValue?.receivedValue || 'null' }}) </span></pre>
                  </span>
                </v-list-item>
              </v-list>
            </v-card-text>
          </span>
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

<script setup>
import { onMounted, toRefs } from 'vue'
import jsonpath from 'jsonpath'
import { useMainStore } from '~/store'
import { REPOSITORY_URL } from '@/constants'
import { isOut, getCaseId, getMessageType, setCaseId, buildMessage, sendMessage } from '~/composables/messageUtils.js'

const store = useMainStore()
const config = useRuntimeConfig()
const selectedRequiredValuesIndex = ref(null)
const currentCaseId = ref(null)
const localCaseId = ref(null)
const { testCase } = toRefs(store)
const currentlySelectedStep = ref(1)
const currentStep = ref(1)
const selectedMessageIndex = ref(0)
const selectedCaseIds = ref([])
const handledLength = ref(0)

useHead({
  titleTemplate: toRef(useMainStore(), 'testHeadTitle')
})

onMounted(() => {
  selectedRequiredValuesIndex.value = null
  currentCaseId.value = null
  localCaseId.value = generateCaseId()
  currentStep.value = 1
  initialize()
})

const clientMessages = computed(() => {
  return store.messages.filter(
    message =>
      (isOut(message.direction) &&
            message.body.senderID === store.user.clientId) ||
          (!isOut(message.direction) &&
            message.routingKey.startsWith(store.user.clientId))
  )
})

const selectedMessage = computed(() => {
  return selectedTypeCaseMessages.value[selectedMessageIndex.value]
})

const showableMessages = computed(() => {
  return store.showSentMessages
    ? clientMessages.value
    : clientMessages.value.filter(message => !isOut(message.direction))
})
const selectedTypeMessages = computed(() => {
  return showableMessages.value
})
const selectedTypeCaseMessages = computed(() => {
  if (selectedCaseIds.value.length === 0) {
    return selectedTypeMessages.value
  }
  return selectedTypeMessages.value.filter(message =>
    selectedCaseIds.value.includes(getCaseId(message, true))
  )
})

async function initialize () {
  await loadJsonSteps()

  if (testCase.value.steps[currentStep.value - 1]?.type === 'receive') {
    submitMessage(testCase.value.steps[currentStep.value - 1])
  }
}

async function loadJsonSteps () {
  for (const step of testCase.value.steps) {
    if (step.type === 'receive') {
      const response = await fetch(REPOSITORY_URL + config.public.modelBranch + '/src/main/resources/sample/examples/' + step.model + '/' + step.file)
      const json = await response.json()
      step.json = json
    }
  }
}

function validateMessage (index, ack, stayOnStep = false) {
  selectedTypeCaseMessages.value.forEach((message, i) => {
    if (i === index) {
      if (!currentCaseId.value) {
        currentCaseId.value = getCaseId(message, true)
      }
      message.validatedStep = currentStep.value - 1
      message.validated = true
      if (ack) {
        if (getMessageType(message) !== 'ack' && message.routingKey.startsWith(store.user.clientId)) {
          const msg = buildAck(message.body.distributionID)
          sendMessage(msg)
        }
      }
    } else if (!message.validated && message.relatedStep === currentStep.value - 1) {
      message.stale = true
    }
  })
  if (!stayOnStep) {
    nextStep()
  }
}

function nextStep () {
  currentStep.value++
  currentlySelectedStep.value = currentStep.value
  if (testCase.value.steps[currentStep.value - 1]?.type === 'receive') {
    submitMessage(testCase.value.steps[currentStep.value - 1])
  }
}

function goToStep (step) {
  currentStep.value = step
  currentlySelectedStep.value = step
}

function submitMessage (step) {
  let message = step.json
  message = replaceValues(message, step.requiredValues)
  if (step.idOverrideProperties) {
    message = overrideIds(message, step.idOverrideProperties)
  }
  if (!currentCaseId.value) {
    currentCaseId.value = localCaseId.value
  }
  setCaseId(message, currentCaseId.value, localCaseId.value)
  const builtMessage = buildMessage(message)
  testCase.value.steps[currentStep.value - 1].awaitedReferenceDistributionID = builtMessage.distributionID
  sendMessage(builtMessage)
}

/**
 * Replaces values in a message using jsonpath:value pairs
 */
function replaceValues (message, requiredValues) {
  requiredValues.forEach((entry) => {
    jsonpath.value(message, entry.path.join('.'), entry.value)
  })
  return message
}

/**
 * Replaces specified values with currently connected client's clientId
 */
function overrideIds (message, idReplacementPaths) {
  for (const path of idReplacementPaths) {
    jsonpath.value(message, path, store.user.clientId)
  }
  return message
}

function generateCaseId () {
  const currentDate = new Date()
  const year = currentDate.getFullYear().toString().slice(-2)
  const dayOfYear = Math.floor((currentDate - new Date(currentDate.getFullYear(), 0, 0)) / (1000 * 60 * 60 * 24)).toString().padStart(3, '0')
  const hour = currentDate.getHours().toString().padStart(2, '0')
  const minutes = currentDate.getMinutes().toString().padStart(2, '0')
  const seconds = currentDate.getSeconds().toString().slice(-1)

  const time = year + dayOfYear + hour + minutes + seconds
  return store.user.clientId + '-' + 'DRMFR15690' + time
}

function getAwaitedValues (step) {
  if (step.type === 'send') {
    const requiredValuesObject = {}
    step.requiredValues.forEach((entry) => {
      requiredValuesObject[entry.path.join('.')] = {
        value: entry.value,
        valid: entry.valid
      }
    })
    return requiredValuesObject
  } else {
    return getAwaitedReferenceDistributionObject(step)
  }
}

function validateStep (stepIndex) {
  testCase.value.steps[stepIndex].validatedReceivedValues = true
  nextStep()
}

function getAwaitedReferenceDistributionIdJson (step) {
  return [
    {
      path: ['$', 'reference', 'distributionID'],
      value: step?.awaitedReferenceDistributionID
    }
  ]
}

function getAwaitedReferenceDistributionObject (step) {
  return {
    '$.reference.distributionID': {
      value: step?.awaitedReferenceDistributionID
    }
  }
}

function flattenObject (ob) {
  const toReturn = {}

  for (const i in ob) {
    if (!Object.prototype.hasOwnProperty.call(ob, i)) { continue }

    if (typeof ob[i] === 'object' && ob[i] !== null) {
      const flatObject = flattenObject(ob[i])
      for (const x in flatObject) {
        if (!Object.prototype.hasOwnProperty.call(flatObject, x)) { continue }
        toReturn[i + '.' + x] = flatObject[x]
      }
    } else {
      toReturn[i] = ob[i]
    }
  }

  return toReturn
}

function checkMessage (message) {
  const currentTestStep = testCase.value.steps[currentStep.value - 1]

  if (currentTestStep.type === 'send') {
    return checkMessageContainsAllRequiredValues(message, currentTestStep.requiredValues)
  } else if (!currentTestStep.validatedAcknowledgement) {
    message.validatedAcknowledgement = checkMessageContainsAllRequiredValues(
      message,
      getAwaitedReferenceDistributionIdJson(currentTestStep)
    )

    currentTestStep.validatedAcknowledgement = message.validatedAcknowledgement
    if (message.validatedAcknowledgement) {
      validateMessage(selectedTypeCaseMessages.value.indexOf(message), false, true)
    }

    return currentTestStep.validatedAcknowledgement && currentTestStep.validatedReceivedValues
  }
}

function checkMessageContainsAllRequiredValues (message, requiredValues) {
  let valid = true
  const validatedValues = []

  requiredValues.forEach(function (element) {
    const result = jsonpath.query(message.body.content[0].jsonContent.embeddedJsonContent.message, element.path.join('.'))
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
  // set(message, 'message', message)
  return valid
}

function getStepColor (index) {
  if (index === currentlySelectedStep.value - 1) {
    return 'primary'
  } else if (index === currentStep.value - 1) {
    return '#CFE2F6'
  } else if (index < currentStep.value - 1) {
    return 'green'
  } else {
    return 'grey'
  }
}

function setSelectedMessage (message) {
  selectedMessageIndex.value = selectedTypeCaseMessages.value.indexOf(message)
}

// Watch the selectedTypeCaseMessages array
watch(selectedTypeCaseMessages, (newMessages) => {
  selectedMessageIndex.value = 0

  // Ensure the current step is within bounds and new messages have arrived
  if (currentStep.value <= testCase.value.steps.length && newMessages.length > 0) {
    // Iterate over new messages starting from the latest added
    for (let i = (newMessages.length - handledLength.value - 1); i >= 0; i--) {
      const lastMessage = newMessages[i]

      // If message is an ack and outgoing, find related message and update relatedStep
      if (getMessageType(lastMessage) === 'ack' && isOut(lastMessage.direction)) {
        const relatedMessage = selectedTypeCaseMessages.value.find(message =>
          message.body?.content[0]?.jsonContent?.embeddedJsonContent?.message?.messageId ===
              lastMessage.body?.content[0]?.jsonContent?.embeddedJsonContent?.message?.reference?.distributionID)
        lastMessage.relatedStep = relatedMessage.relatedStep
      } else {
        lastMessage.relatedStep = currentStep.value - 1
      }

      // Check and validate the message if it's incoming
      if (!lastMessage.isOut) {
        if (checkMessage(lastMessage)) {
          const shouldStayOnStep = testCase.value.steps[currentStep.value - 1].type === 'receive' &&
                !(testCase.value.steps[currentStep.value - 1].validatedAcknowledgement &&
                  testCase.value.steps[currentStep.value - 1].validatedReceivedValues)
          validateMessage(newMessages.indexOf(lastMessage), true, shouldStayOnStep)
        }
      }
    }
    handledLength.value = newMessages.length
  }
}, { deep: true })
</script>

<script>
export default {
  beforeRouteEnter (to, from) {
    if (!useMainStore().isAuthenticated) {
      return { name: 'index' }
    }
  }
}
</script>

<style scoped>
div.v-stepper-header>div.v-col {
  flex-basis: auto;
}
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
  line-break: auto;
  padding-left: 0.5rem;
}

.v-stepper__header {
  height: auto;
}

span.confirmation-buttons>button {
  margin: 6px;
}
</style>
