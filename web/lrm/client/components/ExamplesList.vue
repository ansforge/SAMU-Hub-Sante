<template>
  <div>
    <v-row>
      <v-col cols="auto" class="pr-0 mt-2">
        <v-btn
          variant="outlined"
          rounded="xl"
          size="x-small"
          color="primary"
          icon="mdi-upload"
          :loading="isSelectingUpload"
          @click="handleFileImport"
        />
        <!-- Create a File Input that will be hidden but triggered with JavaScript -->
        <input
          ref="uploader"
          class="d-none"
          type="file"
          @change="onFileChanged"
        >
      </v-col>
      <v-col>
        <v-chip-group
          v-model="selectedExample"
          selected-class="primary--text"
          column
        >
          <v-chip v-for="example in examples" :key="example.name" :value="example" :color="(selectedDetailIndex === name) ? 'primary' : 'secondary'" @click="handleExampleSelection(example)">
            <v-icon>
              {{ example.icon }}
            </v-icon>
            {{ example.name }}
          </v-chip>
        </v-chip-group>
      </v-col>
    </v-row>
    <exampleDetails v-if="selectedExample !== undefined" class="mb-4" v-bind="selectedExample" />
  </div>
</template>

<script>
import { REPOSITORY_URL } from '@/constants'
import { useMainStore } from '~/store'

export default {
  props: {
    source: {
      type: String,
      required: true
    },
    examples: {
      type: Array,
      required: true
    }
  },
  emits: ['exampleLoaded'],
  setup (_, { emit }) {
    const emitExampleLoaded = () => {
      emit('exampleLoaded')
    }
    return {
      emitExampleLoaded
    }
  },
  data () {
    return {
      store: useMainStore(),
      selectedDetailIndex: undefined,
      isSelectingUpload: false,
      selectedExample: {}
    }
  },
  computed: {
    selectedSchema () {
      return this.store.selectedSchema
    }
  },
  watch: {
    selectedSchema () {
      this.selectedExample = {}
      this.selectFirstExample()
    }
  },
  mounted () {
    this.selectFirstExample()
  },
  methods: {
    selectFirstExample () {
      if (this.examples.length > 0) {
        this.handleExampleSelection(this.examples[0])
        this.selectedExample = this.examples[0]
      }
    },
    handleFileImport () {
      this.isSelectingUpload = true

      // After obtaining the focus when closing the FilePicker, return the button state to normal
      window.addEventListener('focus', () => {
        this.isSelectingUpload = false
      }, { once: true })

      // Trigger click on the FileInput
      this.$refs.uploader.click()
    },
    onFileChanged (event) {
      const $this = this
      function onReaderLoad (event) {
        // $this.selectedDetailIndex = undefined
        console.log(event.target.result)
        // We're going to assume that the example we're trying to load starts
        // with a use case property (such as createCase, emsi, etc.) and that
        // it's the first and only property at the root level of the example.
        // TODO: instead of taking the first property and praying, we should extract
        // the name of the use case from somewhere and use it to properly access that
        // property in the parsed json object.
        const parsedJson = JSON.parse(event.target.result)
        this.store.currentMessage = parsedJson[Object.keys(parsedJson)[0]]
      }
      const reader = new FileReader()
      reader.onload = onReaderLoad
      if (event.target.files[0]) {
        reader.readAsText(event.target.files[0])
      }
    },
    loadExample (exampleFilepath) {
      if (exampleFilepath && exampleFilepath.includes(this.store.selectedSchema)) {
        fetch(REPOSITORY_URL + this.source + '/src/main/resources/sample/examples/' + exampleFilepath)
          .then(response => response.json())
          .then((data) => {
            this.store.currentMessage = data[Object.keys(data)[0]]
            this.emitExampleLoaded()
          })
      } else {
        this.store.currentMessage = {}
        this.emitExampleLoaded()
      }
    },
    handleExampleSelection (example) {
      // If currently selected example is clicked, deselect it
      if (this.selectedExample?.file === example?.file) {
        this.loadExample(null)
      } else {
        this.loadExample(example.file)
      }
    }
  }
}
</script>
