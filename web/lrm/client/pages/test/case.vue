<template>
  <v-row justify="center">
    <v-col cols="12" sm="7">
      <v-card class="main-card" style="height: 86vh">
        <v-card-title class="d-flex text-h5 pb-0">
          Cas de test
          <span class="font-weight-bold mr-5 mb-4"
            >&nbsp;{{ testCase.label }}
          </span>
          <vhost-selector class="mr-5 mb-4" />
          <v-btn
            v-if="testCase?.steps[currentStep]?.type === 'receive'"
            color="primary"
            @click="submitMessage(testCase?.steps[currentStep])"
          >
            Re-envoyer le message
          </v-btn>
        </v-card-title>
        <v-card-actions class="pt-0" style="flex-direction: column">
          <v-container class="pt-0" full-width>
            <v-stepper v-model="currentStep" class="stepper">
              <v-stepper-header>
                <template
                  v-for="(step, index) in testCase?.steps"
                  :key="'step' + index"
                >
                  <v-col>
                    <v-stepper-item
                      :key="index"
                      :value="step.id"
                      editable
                      style="cursor: pointer"
                      :color="getStepColor(index)"
                      :step="index + 1"
                      edit-icon="mdi-circle"
                      @click="goToStep(index)"
                    >
                      {{ step.label }}
                    </v-stepper-item>
                  </v-col>
                  <v-divider
                    v-if="index < testCase?.steps.length - 1"
                    :key="'divider' + index"
                  />
                </template>
              </v-stepper-header>
            </v-stepper>
          </v-container>
          <v-container>
            <span>
              {{ testCase?.steps[currentlySelectedStep]?.description }}
            </span>
          </v-container>
        </v-card-actions>
        <v-card-text class="main-card-content">
          <v-container full-width>
            <v-row>
              <v-col class="small-message">
                <template v-for="(message, index) in selectedTypeCaseMessages">
                  <div
                    v-if="message.relatedStep === currentlySelectedStep"
                    :key="'wrapper' + index"
                    class="d-flex flex-column flex-wrap pb-1 pt-1"
                    @click="setSelectedMessage(message)"
                  >
                    <ReceivedMessage
                      :key="message.time"
                      :dense="true"
                      :validated-values-count="
                        message?.validatedValues?.filter((value) => value.valid)
                          .length
                      "
                      :required-values-count="
                        testCase?.steps[message.relatedStep]?.type === 'send'
                          ? testCase?.steps[message.relatedStep]?.requiredValues
                              ?.length
                          : 1
                      "
                      v-bind="message"
                      class="message mb-4"
                      :class="{
                        stale: message.stale,
                        selected: selectedMessageIndex === index,
                      }"
                    />
                  </div>
                </template>
              </v-col>
              <v-col
                v-if="selectedMessage?.relatedStep === currentlySelectedStep"
                class="full-message"
              >
                <!-- Details of selected message (last received or sent by default)-->
                <ReceivedMessage
                  v-if="selectedMessage"
                  :key="selectedMessage?.time"
                  :json-depth="10"
                  :required-values-count="
                    testCase?.steps[selectedMessage.relatedStep]?.type ===
                    'send'
                      ? testCase?.steps[selectedMessage.relatedStep]
                          ?.requiredValues?.length
                      : 1
                  "
                  v-bind="selectedMessage"
                  class="message mb-4"
                  :class="{ stale: selectedMessage?.stale }"
                />
              </v-col>
            </v-row>
          </v-container>
        </v-card-text>
      </v-card>
    </v-col>
    <v-col cols="12" sm="5">
      <v-card class="main-card" style="height: 86vh; overflow-y: auto">
        <template v-if="currentStep < testCase?.steps.length">
          <span>
            <v-card-title class="d-flex justify-space-between">
              <!-- Button that validates the step and goes to the next -->
              <v-btn color="primary" @click="validateStep(currentStep)">
                Passer à l'étape suivante
              </v-btn>
              <span id="result-counter">
                <span class="text-grey">{{ getCounts().unreviewed }}</span> -
                <span class="text-success">{{ getCounts().valid }}</span> -
                <span class="text-warning">{{ getCounts().approximate }}</span>
                - <span class="text-error">{{ getCounts().invalid }}</span> -
                {{ getCounts().total }}
              </span>
            </v-card-title>
            <v-card-title>
              {{
                testCase?.steps[currentStep]?.type === 'send'
                  ? 'Valeurs attendues dans le message'
                  : "Valeurs attendues dans l'acquittement"
              }}
            </v-card-title>
            <v-card-text>
              <p v-if="getAwaitedValues(testCase?.steps[currentStep]) === null">
                En attente de la réception de l'ID de distribution...
              </p>
              <v-list>
                <v-list-item
                  v-for="(requiredValue, name, index) in getAwaitedValues(
                    testCase?.steps[currentStep]
                  )"
                  :key="'requiredValue' + index"
                >
                  <div class="d-flex">
                    <span>
                      <v-icon
                        v-if="
                          requiredValue.value &&
                          requiredValue?.valid === 'valid'
                        "
                        style="flex: 0"
                        color="success"
                      >
                        mdi-check
                      </v-icon>

                      <v-icon v-else style="flex: 0" color="error">
                        mdi-close
                      </v-icon>
                    </span>
                    <span>
                      <pre><b>{{ name }}:</b></pre>
                      <pre>{{ requiredValue.value }}</pre>
                      <pre
                        v-if="requiredValue?.valid !== ValidationStatus.VALID"
                        class="wrong-received"
                      >
(Reçu: {{ requiredValue?.receivedValue || 'null' }}) </pre
                      >
                    </span>
                  </div>
                </v-list-item>
              </v-list>
            </v-card-text>

            <div v-if="testCase?.steps[currentStep]?.type === 'receive'">
              <v-card-title> Valeurs reçues dans le message </v-card-title>
              <v-card-text>
                <v-list>
                  <!-- Generate a list of paths:values from required values and add three buttons for each entry, letting user indicate whether the value they received is correct, 'somewhat' correct or incorrect-->
                  <v-list-item
                    v-for="(requiredValue, index) in testCase?.steps[
                      currentStep
                    ].requiredValues"
                    :key="'requiredValue' + index"
                    class="received-values-list"
                    :data-index="index"
                  >
                    <span class="d-flex flex-row align-center">
                      <span class="confirmation-buttons d-flex flex-row">
                        <!-- Grey out buttons that do not correspond to the validation state if the value has already been validated -->
                        <v-btn
                          density="compact"
                          icon="mdi-check"
                          :color="
                            requiredValue.valid === 'valid' ? 'success' : 'grey'
                          "
                          @click="
                            setValidationStatus(requiredValue, 'valid', index)
                          "
                        />
                        <v-btn
                          density="compact"
                          icon="mdi-tilde"
                          :color="
                            requiredValue.valid === 'approximate'
                              ? 'warning'
                              : 'grey'
                          "
                          @click="
                            setValidationStatus(
                              requiredValue,
                              'approximate',
                              index
                            )
                          "
                        />
                        <v-btn
                          density="compact"
                          icon="mdi-close"
                          :color="
                            requiredValue.valid === 'invalid' ? 'error' : 'grey'
                          "
                          @click="
                            setValidationStatus(requiredValue, 'invalid', index)
                          "
                        />
                      </span>
                      <span>
                        <pre
                          class="values"
                          :style="{
                            color:
                              requiredValue.valid === 'valid'
                                ? 'green'
                                : requiredValue.valid === 'approximate'
                                ? 'orange'
                                : requiredValue.valid === 'invalid'
                                ? 'red'
                                : 'black',
                          }"
                        ><b>{{ requiredValue.path.join('.') }}:</b> <br>{{ requiredValue.value }}</pre>
                      </span>
                    </span>
                    <v-text-field
                      :ref="'commentField' + index"
                      v-model="requiredValue.description"
                      :class="[
                        'mt-2',
                        'comment-field',
                        {
                          'd-block':
                            requiredValue.valid &&
                            requiredValue.valid !== ValidationStatus.VALID,
                        },
                      ]"
                      :data-valid="requiredValue.valid"
                      :data-id="requiredValue.path.join('.')"
                      label="Commentaire"
                    />
                  </v-list-item>
                </v-list>
              </v-card-text>
            </div>
          </span>
        </template>
        <template v-else>
          <v-card-title> Fin du cas de test </v-card-title>
          <v-card-text class="main-card-content">
            <v-card-text>
              <p>Récapitulatif des resultats de test:</p>
              <v-list class="results">
                <v-list-item class="d-flex flex-row">
                  <v-list-item-title
                    >Nombre de valeurs attendues:
                    {{ getTotalCounts().total }}</v-list-item-title
                  >
                </v-list-item>
                <v-list-item>
                  <v-list-item-title
                    >Nombre de valeurs reçues:
                    {{
                      getTotalCounts().valid +
                      getTotalCounts().approximate +
                      getTotalCounts().invalid
                    }}</v-list-item-title
                  >
                </v-list-item>
                <v-list-item>
                  <v-list-item-title
                    >Nombre de valeurs correctes:
                    {{ getTotalCounts().valid }}</v-list-item-title
                  >
                </v-list-item>
                <v-list-item>
                  <v-list-item-title
                    >Nombre de valeurs approximatives:
                    {{ getTotalCounts().approximate }}</v-list-item-title
                  >
                </v-list-item>
                <v-list-item>
                  <v-list-item-title
                    >Nombre de valeurs incorrectes:
                    {{ getTotalCounts().invalid }}</v-list-item-title
                  >
                </v-list-item>
              </v-list>
            </v-card-text>
          </v-card-text>
          <v-btn color="primary" @click="generatePdf">Générer le PDF</v-btn>
        </template>
      </v-card>
    </v-col>
  </v-row>
</template>

<script setup>
import { onMounted, toRefs, ref, toRef, computed, watch } from 'vue';
import jsonpath from 'jsonpath';
import { generateCasePdf } from '../../composables/generateCasePdf';
import mixinWebsocket from '~/mixins/mixinWebsocket';
import { useMainStore } from '~/store';
import { useAuthStore } from '@/store/auth';
import { REPOSITORY_URL } from '@/constants';
import {
  isOut,
  getCaseId,
  getMessageType,
  setCaseId,
  buildMessage,
  sendMessage,
  getDistributionIdOfAckedMessage,
  ValidationStatus,
  buildAck,
} from '~/composables/messageUtils.js';

const store = useMainStore();
const authStore = useAuthStore();
const selectedRequiredValuesIndex = ref(null);
const currentCaseId = ref(null);
const localCaseId = ref(null);
const { testCase } = toRefs(store);
const currentlySelectedStep = ref(0);
const currentStep = ref(0);
const selectedMessageIndex = ref(0);
const selectedCaseIds = ref([]);
const handledLength = ref(0);

// eslint-disable-next-line no-undef
useHead({
  titleTemplate: toRef(useMainStore(), 'testHeadTitle'),
});

onMounted(() => {
  selectedRequiredValuesIndex.value = null;
  currentCaseId.value = null;
  localCaseId.value = generateCaseId();
  currentStep.value = 0;
  initialize();
});

const clientMessages = computed(() => {
  return store.messages.filter(
    (message) =>
      (isOut(message.direction) &&
        message.body.senderID === authStore.user.clientId) ||
      (!isOut(message.direction) &&
        message.routingKey.startsWith(authStore.user.clientId))
  );
});

const selectedMessage = computed(() => {
  return selectedTypeCaseMessages.value[selectedMessageIndex.value];
});

const showableMessages = computed(() => {
  return store.showSentMessages
    ? clientMessages.value
    : clientMessages.value.filter((message) => !isOut(message.direction));
});
const selectedTypeMessages = computed(() => {
  return showableMessages.value;
});
const selectedTypeCaseMessages = computed(() => {
  if (selectedCaseIds.value.length === 0) {
    return selectedTypeMessages.value;
  }

  return selectedTypeMessages.value.filter((message) =>
    selectedCaseIds.value.includes(getCaseId(message, true))
  );
});

async function initialize() {
  await loadJsonSteps();

  if (testCase.value.steps[currentStep.value]?.type === 'receive') {
    submitMessage(testCase.value.steps[currentStep.value]);
  }
}

async function loadJsonSteps() {
  for (const step of testCase.value.steps) {
    if (step.type === 'receive') {
      const response = await fetch(
        REPOSITORY_URL +
          store.selectedVhost.modelVersion +
          '/src/main/resources/sample/examples/' +
          step.model +
          '/' +
          step.file
      );
      const json = await response.json();
      step.json = json;
    }
  }
}

function validateMessage(index, ack, stayOnStep = false) {
  selectedTypeCaseMessages.value.forEach((message, i) => {
    if (i === index) {
      if (!currentCaseId.value) {
        currentCaseId.value = getCaseId(message, true);
      }
      message.validatedStep = currentStep.value;
      message.validated = true;
      if (ack) {
        if (
          getMessageType(message) !== 'ack' &&
          message.routingKey.startsWith(authStore.user.clientId)
        ) {
          const msg = buildAck(message.body.distributionID);
          sendMessage(msg);
        }
      }
    } else if (
      !message.validated &&
      message.relatedStep === currentStep.value
    ) {
      message.stale = true;
    }
  });
  if (!stayOnStep) {
    nextStep();
  }
}

function nextStep() {
  currentStep.value++;
  currentlySelectedStep.value = currentStep.value;
  if (testCase.value.steps[currentStep.value]?.type === 'receive') {
    submitMessage(testCase.value.steps[currentStep.value]);
  }
}

function goToStep(step) {
  currentStep.value = step;
  currentlySelectedStep.value = step;
}

function submitMessage(step) {
  let message = step.json;
  message = replaceValues(message, step.requiredValues);
  if (step.idOverrideProperties) {
    message = overrideIds(message, step.idOverrideProperties);
  }
  if (!currentCaseId.value) {
    currentCaseId.value = localCaseId.value;
  }
  setCaseId(message, currentCaseId.value, localCaseId.value);
  const builtMessage = buildMessage(message);
  testCase.value.steps[currentStep.value].awaitedReferenceDistributionID =
    builtMessage.distributionID;
  sendMessage(builtMessage);
}

/**
 * Replaces values in a message using jsonpath:value pairs
 */
function replaceValues(message, requiredValues) {
  requiredValues.forEach((entry) => {
    jsonpath.value(message, entry.path.join('.'), entry.value);
  });
  return message;
}

/**
 * Replaces specified values with currently connected client's clientId
 */
function overrideIds(message, idReplacementPaths) {
  for (const path of idReplacementPaths) {
    jsonpath.value(message, path, authStore.user.clientId);
  }
  return message;
}

function generateCaseId() {
  const currentDate = new Date();
  const year = currentDate.getFullYear().toString().slice(-2);
  const dayOfYear = Math.floor(
    (currentDate - new Date(currentDate.getFullYear(), 0, 0)) /
      (1000 * 60 * 60 * 24)
  )
    .toString()
    .padStart(3, '0');
  const hour = currentDate.getHours().toString().padStart(2, '0');
  const minutes = currentDate.getMinutes().toString().padStart(2, '0');
  const seconds = currentDate.getSeconds().toString().slice(-1);

  const time = year + dayOfYear + hour + minutes + seconds;
  return authStore.user.clientId + '.' + 'DRMFR15690' + time;
}

function getAwaitedValues(step) {
  if (step.type === 'send') {
    const requiredValuesObject = {};
    step.requiredValues.forEach((entry) => {
      requiredValuesObject[entry.path.join('.')] = {
        value: entry.value,
        valid: entry.valid,
      };
    });
    return requiredValuesObject;
  } else {
    return getAwaitedReferenceDistributionObject(step);
  }
}

function validateStep(stepIndex) {
  testCase.value.steps[stepIndex].validatedReceivedValues = true;
  nextStep();
}

function getAwaitedReferenceDistributionIdJson(step) {
  return [
    {
      path: ['$', 'reference', 'distributionID'],
      value: step?.awaitedReferenceDistributionID,
    },
  ];
}

function getAwaitedReferenceDistributionObject(step) {
  const currentStepSelectedTypeMessages = selectedTypeCaseMessages.value.filter(
    (message) => message.relatedStep === currentStep.value
  );
  const lastCurrentStepAckedDistributionID = currentStepSelectedTypeMessages[0]
    ? getDistributionIdOfAckedMessage(currentStepSelectedTypeMessages[0])
    : null;
  const validationStatus =
    lastCurrentStepAckedDistributionID === step?.awaitedReferenceDistributionID
      ? ValidationStatus.VALID
      : lastCurrentStepAckedDistributionID
      ? ValidationStatus.APPROXIMATE
      : ValidationStatus.INVALID;

  return {
    '$.reference.distributionID': {
      value: step?.awaitedReferenceDistributionID,
      valid: validationStatus,
      receivedValue:
        getDistributionIdOfAckedMessage(currentStepSelectedTypeMessages[0]) ??
        'N/A',
    },
  };
}

function checkMessage(message) {
  const currentTestStep = testCase.value.steps[currentStep.value];

  if (currentTestStep?.type === 'send') {
    return checkMessageContainsAllRequiredValues(
      message,
      currentTestStep.requiredValues
    );
  } else {
    message.validatedAcknowledgement = checkMessageContainsAllRequiredValues(
      message,
      getAwaitedReferenceDistributionIdJson(currentTestStep)
    );

    currentTestStep.validatedAcknowledgement = message.validatedAcknowledgement;
    if (message.validatedAcknowledgement) {
      validateMessage(
        selectedTypeCaseMessages.value.indexOf(message),
        false,
        true
      );
    }

    return (
      currentTestStep.validatedAcknowledgement &&
      currentTestStep.validatedReceivedValues
    );
  }
}

function checkMessageContainsAllRequiredValues(message, requiredValues) {
  let valid = true;
  const validatedValues = [];

  requiredValues.forEach(function (element) {
    const result = jsonpath.query(
      message.body.content[0].jsonContent.embeddedJsonContent.message,
      element.path.join('.')
    );

    if (result.length === 0 || !result.includes(element.value)) {
      valid = false;
      element.valid = ValidationStatus.INVALID;
      validatedValues.push({
        valid: false,
        value: element,
        receivedValue: result[0],
      });
    } else {
      validatedValues.push({ valid: true, value: element });
      element.valid = ValidationStatus.VALID;
    }
  });

  message.validatedValues = JSON.parse(JSON.stringify(validatedValues));
  // set(message, 'message', message)
  return valid;
}

function getStepColor(index) {
  // For reception steps, color is determined by the average color of the received values
  if (testCase.value.steps[index].type === 'receive') {
    const counts = getCounts(testCase.value.steps[index]);
    return getAverageColor(
      counts.unreviewed,
      counts.valid,
      counts.approximate,
      counts.invalid,
      counts.total
    );
  } else {
    // For send steps, color is determined by the validation state of the step
    return testCase.value.steps[index]?.validatedReceivedValues
      ? 'success'
      : 'grey';
  }
}

function getAverageColor(unset, success, warning, error, total) {
  // grey: #9e9e9e, success: #4caf50, warning: #fb8c00, error: #b00020, total: #000000
  const unsetPercent = (unset / total) * 100;
  const successPercent = (success / total) * 100;
  const warningPercent = (warning / total) * 100;
  const errorPercent = (error / total) * 100;

  const red = Math.round(
    (unsetPercent * 158 +
      successPercent * 76 +
      warningPercent * 251 +
      errorPercent * 176) /
      100
  );
  const green = Math.round(
    (unsetPercent * 158 +
      successPercent * 175 +
      warningPercent * 140 +
      errorPercent * 0) /
      100
  );
  const blue = Math.round(
    (unsetPercent * 158 +
      successPercent * 80 +
      warningPercent * 0 +
      errorPercent * 32) /
      100
  );

  return `rgb(${red}, ${green}, ${blue})`;
}

function setSelectedMessage(message) {
  selectedMessageIndex.value = selectedTypeCaseMessages.value.indexOf(message);
}

function getCounts(step = testCase.value.steps[currentStep.value]) {
  const requiredValues = step.requiredValues;

  return {
    total: requiredValues.length,
    unreviewed: requiredValues.filter((value) => value.valid === undefined)
      .length,
    valid: requiredValues.filter(
      (value) => value.valid === ValidationStatus.VALID
    ).length,
    approximate: requiredValues.filter(
      (value) => value.valid === ValidationStatus.APPROXIMATE
    ).length,
    invalid: requiredValues.filter(
      (value) => value.valid === ValidationStatus.INVALID
    ).length,
  };
}

function getTotalCounts() {
  let total = 0;
  let valid = 0;
  let approximate = 0;
  let invalid = 0;

  for (const step of testCase.value.steps) {
    if (step.type === 'receive') {
      const counts = getCounts(step);
      total += counts.total;
      valid += counts.valid;
      approximate += counts.approximate;
      invalid += counts.invalid;
    }
  }

  return {
    total,
    valid,
    approximate,
    invalid,
  };
}

const generatePdf = () =>
  generateCasePdf(testCase, store, authStore, getCounts);

// Watch the selectedTypeCaseMessages array
watch(
  selectedTypeCaseMessages,
  (newMessages) => {
    selectedMessageIndex.value = 0;

    // Ensure the current step is within bounds and new messages have arrived
    if (
      currentStep.value <= testCase.value.steps.length &&
      newMessages.length > 0
    ) {
      // Iterate over new messages starting from the latest added
      for (let i = newMessages.length - handledLength.value - 1; i >= 0; i--) {
        const lastMessage = newMessages[i];
        checkMessage(lastMessage);

        // If message is an ack and outgoing, find related message and update relatedStep
        if (
          getMessageType(lastMessage) === 'ack' &&
          isOut(lastMessage.direction)
        ) {
          const relatedMessage = selectedTypeCaseMessages.value.find(
            (message) =>
              getDistributionIdOfAckedMessage(message) ===
              getDistributionIdOfAckedMessage(lastMessage)
          );
          lastMessage.relatedStep = relatedMessage.relatedStep;
        } else {
          lastMessage.relatedStep = currentStep.value;
        }

        // Check and validate the message if it's incoming
        if (!lastMessage.isOut) {
          const shouldStayOnStep =
            testCase.value.steps[currentStep.value].type === 'receive' &&
            !(
              testCase.value.steps[currentStep.value]
                .validatedAcknowledgement &&
              testCase.value.steps[currentStep.value].validatedReceivedValues
            );
          validateMessage(
            newMessages.indexOf(lastMessage),
            true,
            shouldStayOnStep
          );
        }
      }
      handledLength.value = newMessages.length;
    }
  },
  { deep: true }
);
</script>

<script>
export default {
  name: 'Testcase',
  mixins: [mixinWebsocket],
  methods: {
    setValidationStatus(requiredValue, status, index) {
      requiredValue.valid =
        requiredValue.valid === status ? undefined : status;
      const commentFieldRef = this.$refs['commentField' + index];
      if (!commentFieldRef) return;
      const commentField = commentFieldRef[0];
      if (requiredValue.valid === ValidationStatus.VALID) {
        commentField.savedDescription = requiredValue.description;
        requiredValue.description = '';
      } else if (commentField.savedDescription) {
        requiredValue.description = commentField.savedDescription;
      }

      this.$nextTick(() => {
        commentField.focus();
      });
    },
  },
};
</script>

<style scoped>
div.v-stepper-header > div.v-col {
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

.v-card .v-card-title {
  display: flex;
  flex-wrap: wrap;
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

.main-card-content {
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

span.confirmation-buttons > button {
  margin: 6px;
}

.comment-field {
  display: none;
}
</style>
