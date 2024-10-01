<template>
  <v-row justify="center">
    <v-col cols="12" sm="12">
      <v-card-title class="d-flex text-h5">
        Sélection de cas de test
        <vhost-selector class="mr-5" />
      </v-card-title>
      <v-list>
        <v-list-item
          v-for="(category,categoryIndex) in testCases"
          :key="category.categoryLabel+'-'+categoryIndex"
          class="flex-column align-baseline"
        >
          <v-card-title class="text-h5">
            {{ category.categoryLabel }}
          </v-card-title>
          <v-expansion-panels>
            <v-expansion-panel
              v-for="(testCase, caseIndex) in category.testCases"
              :key="testCase.label + '-' + caseIndex"
            >
              <v-expansion-panel-title class="ma-0 pa-0 pr-10">
                <div style="flex: 1;">
                  <v-list-item-title>
                    <v-card-title class="pt-0 pb-0">
                      {{ testCase.label }}
                    </v-card-title>
                  </v-list-item-title>
                  <v-list-item-subtitle>
                    <v-card-text class="pt-1 pb-0">
                      {{ testCase.description }}
                    </v-card-text>
                  </v-list-item-subtitle>
                </div>
                <div style="flex: 0;">
                  <v-btn
                    class="ml-3 mr-3"
                    style="flex-grow: 0; max-width: fit-content;"
                    color="primary"
                    @click="goToTestCase(testCase)"
                  >
                    Sélectionner
                  </v-btn>
                </div>
              </v-expansion-panel-title>
              <v-expansion-panel-text>
                <v-card-title>
                  Pas de test
                </v-card-title>
                <v-list>
                  <v-timeline side="end">
                    <v-timeline-item
                      v-for="(step, index) in testCase.steps"
                      :key="step.label"
                      :dot-color="step.type === 'send' ? 'secondary' : 'primary'"
                      :icon="step.type === 'send' ? 'mdi-upload' : 'mdi-download'"
                    >
                      <template v-if="step.type === 'send'">
                        <v-card class="test-step-card">
                          <v-card-title>
                            {{ index + 1 }}. {{ step.label }}
                          </v-card-title>
                          <v-card-subtitle>
                            {{ step.description }}
                          </v-card-subtitle>
                          <v-card-text>
                            <ul v-for="requiredValue in step.requiredValues" :key="requiredValue.index">
                              <li>
                                {{ requiredValue.path.join('.') }} : {{ requiredValue.value }}
                              </li>
                            </ul>
                          </v-card-text>
                        </v-card>
                      </template>
                      <template v-if="step.type === 'receive'" #opposite>
                        <v-card class="test-step-card">
                          <v-card-title>
                            {{ index + 1 }}. {{ step.label }}
                          </v-card-title>
                          <v-card-subtitle>
                            {{ step.description }}
                          </v-card-subtitle>
                          <v-card-text>
                            <ul v-for="requiredValue in step.requiredValues" :key="requiredValue.index">
                              <li>
                                {{ requiredValue.path.join('.') }} : {{ requiredValue.value }}
                              </li>
                            </ul>
                          </v-card-text>
                        </v-card>
                      </template>
                    </v-timeline-item>
                  </v-timeline>
                </v-list>
              </v-expansion-panel-text>
            </v-expansion-panel>
          </v-expansion-panels>
        </v-list-item>
      </v-list>
    </v-col>
  </v-row>
</template>

<script setup>
import jsonpath from 'jsonpath'
import testCaseFile from '~/assets/test-cases.json'
import { REPOSITORY_URL } from '@/constants'
import mixinUser from '~/mixins/mixinUser'
import { useMainStore } from '~/store'

const store = useMainStore()
const config = useRuntimeConfig()
const router = useRouter()

const testCaseFileAuto = ref([])
const testCases = ref([])

useHead({
  title: toRef(useMainStore(), 'testHeadTitle')
})

onMounted(() => {
  initialize()
})

async function initialize () {
  await fetchGeneratedTestCases()
  loadTestCases()
}
async function fetchGeneratedTestCases () {
  const response = await fetch(REPOSITORY_URL + config.public.modelBranch + '/csv_parser/out/test_cases.json')
  if (response.ok) {
    testCaseFileAuto.value = await response.json()
  }
}
/**
     * Copies the test cases from the JSON file to the component data,
     * resetting any potential changes to the test cases made during
     * test execution.
     */
async function loadTestCases () {
  const parsedTestCases = []
  for (const category of testCaseFileAuto.value) {
    const newTestCases = ref([])
    for (const testCase of category.testCases) {
      const newTestCase = JSON.parse(JSON.stringify(testCase))
      newTestCase.label = `${newTestCase.label}`
      newTestCase.description = `${newTestCase.description}`
      // We load the example files for test case steps
      for (const step of newTestCase.steps) {
        const response = await fetch(REPOSITORY_URL + config.public.modelBranch + '/src/main/resources/sample/examples/' + step.model + '/' + step.file)
        const receivedMessage = await response.json()
        // We transform the received message json to an array of jsonpaths
        let jsonpaths = []
        jsonpath.nodes(receivedMessage, '$..*').forEach((path) => {
          jsonpaths.push(path)
        })
        // We're only interested in paths with simple values (no objects, but arrays are allowed)
        jsonpaths = jsonpaths.filter((value) => {
          return typeof value.value !== 'object'
        })
        // We filter out the properties in 'ignoredValues' from the jsonpaths array
        jsonpaths = jsonpaths.filter((value) => {
          return !step.ignoredValues.includes(value)
        })
        // We filter out datetime properties from the jsonpaths array using a regex \d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[+-]\d{2}:\d{2}
        jsonpaths = jsonpaths.filter((value) => {
          return !/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[+-]\d{2}:\d{2}/.test(value.value)
        })
        step.json = receivedMessage
        step.requiredValues = jsonpaths
      }
      newTestCases.value.push(newTestCase)
    }
    parsedTestCases.push({
      categoryLabel: category.categoryLabel,
      testCases: newTestCases.value
    })
  }
  testCases.value = [
    ...JSON.parse(JSON.stringify(testCaseFile)),
    ...JSON.parse(JSON.stringify(parsedTestCases))
  ]
}
function goToTestCase (testCase) {
  store.resetMessages()
  // Vue3 breaking change: [Vue Router warn]: Discarded invalid param(s) "testCase" when navigating.
  // See https://github.com/vuejs/router/blob/main/packages/router/CHANGELOG.md#414-2022-08-22 for more details.
  // So we just store the selected test case in the store and navigate to the test case page.
  store.testCase = testCase
  router.push({
    name: 'test-case'
  })
}
</script>

<script>
export default {
  mixins: [mixinUser]
}
</script>
<style>
div.v-card.test-step-card {
  padding: 1rem;
  margin: 1rem;
}
div.v-timeline--vertical{
  justify-content: start;
}
div.v-card-subtitle {
  white-space: normal;
}
div.v-list-item-subtitle {
  margin-bottom: 0.5rem;
}
div.v-list-item-title {
  margin-top: 0.5rem;
}
</style>
