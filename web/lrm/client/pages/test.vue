<template>
  <v-row justify="center">
    <v-col cols="12" sm="12">
      <v-card-title class="headline pb-">
        Sélection de cas de test
      </v-card-title>
      <v-list>
        <v-expansion-panels>
          <v-expansion-panel
            v-for="testCase in testCases"
            :key="testCase.label"
          >
            <v-expansion-panel-header>
              <v-list-item-content>
                <v-list-item-title><v-card-title>{{ testCase.label }}</v-card-title></v-list-item-title>
                <v-list-item-subtitle><v-card-text>{{ testCase.description }}</v-card-text></v-list-item-subtitle>
              </v-list-item-content>
              <v-spacer />
              <v-btn
                color="primary"
                @click="goToTestCase(testCase)"
              >
                Sélectionner
              </v-btn>
            </v-expansion-panel-header>
            <v-expansion-panel-content>
              <v-card-title>
                Pas de test:
              </v-card-title>
              <v-list>
                <v-timeline>
                  <v-timeline-item v-for="step, index in testCase.steps" :key="step.label" :left="step.type === 'receive'" :right="step.type !== 'receive'">
                    <v-list-item>
                      <v-list-item-content>
                        <v-list-item-title>{{ index + 1 }}. {{ step.label }}</v-list-item-title>
                        <exampleDetails v-bind="step.message" />
                      </v-list-item-content>
                    </v-list-item>
                  </v-timeline-item>
                </v-timeline>
              </v-list>
            </v-expansion-panel-content>
          </v-expansion-panel>
        </v-expansion-panels>
      </v-list>
    </v-col>
  </v-row>
</template>

<script>

export default {
  name: 'Test',
  data () {
    return {
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
    this.loadTestCases()
  },
  methods: {
    async loadTestCases () {
      try {
        const response = await this.$axios.get('/test-cases/Receive-EDA-DC-Send-RDC.json')
        this.testCases.push(response.data)
      } catch (error) {
        console.error(error)
      }
    },
    goToTestCase (testCase) {
      this.$router.push({
        name: 'testcase',
        params: {
          testCase
        }
      })
    }
  }
}
</script>
