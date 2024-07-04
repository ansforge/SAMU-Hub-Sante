<template>
  <v-row justify="center">
    <v-col cols="12" sm="12">
      <v-card-title class="text-h5">
        Sélection de cas de test
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
              @click="loadTestCaseJsons(testCase)"
            >
              <v-expansion-panel-title class="ma-0 pa-0 pr-10">
                <v-list-item-content class="flex-row">
                  <div style="flex: 0;">
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
                </v-list-item-content>
              </v-expansion-panel-title>
              <v-expansion-panel-text>
                <v-card-title>
                  Pas de test
                </v-card-title>
                <v-list>
                  <v-timeline>
                    <v-timeline-item
                      v-for="(step, index) in testCase.steps"
                      :key="step.label"
                      :left="step.type === 'send'"
                      :right="step.type !== 'send'"
                      :icon="step.type === 'send' ? 'mdi-upload' : 'mdi-download'"
                    >
                      <v-card>
                        <v-card-title>
                          {{ index + 1 }}. {{ step.label }}
                        </v-card-title>
                        <v-card-subtitle>
                          {{ step.description }}
                        </v-card-subtitle>
                        <v-card-text>
                          <ul v-for="requiredValue in step.message.requiredValues" :key="requiredValue.index">
                            <li>
                              {{ requiredValue.path }} : {{ requiredValue.value }}
                            </li>
                          </ul>
                        </v-card-text>
                      </v-card>
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

<script>

import testCaseFile from '~/assets/test-cases.json'
import { REPOSITORY_URL } from '@/constants'
import mixinUser from '~/mixins/mixinUser'
import { useMainStore } from '~/store'

export default {
  mixins: [mixinUser],
  name: 'Test',
  data () {
    return {
      store: useMainStore(),
      testCaseFileAuto: [],
      testCases: []
    }
  },

  head () {
    return {
      title: `Test [${this.userInfos.name}]`
    }
  },
  computed: {

  },
  mounted () {
    this.initialize()
  },
  methods: {
    async initialize () {
      await this.fetchGeneratedTestCases()
      this.loadTestCases()
    },
    async fetchGeneratedTestCases () {
      const response = await fetch(REPOSITORY_URL + $config.public.modelBranch + '/csv_parser/out/test_cases.json')
      if (response.ok) {
        this.testCaseFileAuto = await response.json()
      }
    },
    /**
     * Copies the test cases from the JSON file to the component data,
     * resetting any potential changes to the test cases made during
     * test execution.
     */
    loadTestCases () {
      // Generated test cases have 3 levels of verification for each required property, ergo we create 3 test cases from each generated test case (Adding 'Level 1/2/3' to the
      // test case label and description)
      const parsedTestCases = []
      this.testCaseFileAuto.forEach((category) => {
        const newTestCases = []
        category.testCases.forEach((testCase) => {
          for (let i = 1; i <= 3; i++) {
            const newTestCase = JSON.parse(JSON.stringify(testCase))
            newTestCase.label = `${newTestCase.label} - Level ${i}`
            newTestCase.description = `${newTestCase.description} - Level ${i}`
            // We only keep the required values for the current and previous levels
            newTestCase.steps.forEach((step) => {
              step.message.requiredValues = step.message.requiredValues.filter(requiredValue => requiredValue.verificationLevel <= i)
            })
            newTestCases.push(newTestCase)
          }
        })
        parsedTestCases.push({
          categoryLabel: category.categoryLabel,
          testCases: newTestCases
        })
      })
      this.testCases = [
        ...JSON.parse(JSON.stringify(testCaseFile)),
        ...JSON.parse(JSON.stringify(parsedTestCases))
      ]
    },
    loadTestCaseJsons (testCase) {
      testCase.steps.forEach(async (step) => {
        if (step.type === 'receive') {
          const response = await fetch(REPOSITORY_URL + $config.public.modelBranch + '/src/main/resources/sample/examples/' + step.message.file)
          const json = await response.json()
          this.$set(step, 'json', json)
        }
      })
    },
    goToTestCase (testCase) {
      this.store.resetMessages()
      // TODO: REMOVE THIS
      // this.$store.dispatch('resetMessages')
      this.navigateTo({
        name: 'test-case',
        params: {
          testCase
        }
      })
    }
  }
}
</script>
<style>
</style>
