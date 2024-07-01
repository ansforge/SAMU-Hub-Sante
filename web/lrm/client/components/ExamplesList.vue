<template>
  <div>
    <v-row>
      <v-col cols="auto" class="pr-0 mt-2">
        <v-btn
          fab
          variant="outlined"
          size="x-small"
          color="primary"
          :loading="isSelectingUpload"
          @click="handleFileImport"
        >
          <v-icon>mdi-upload</v-icon>
        </v-btn>
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
          v-model="selectedDetailIndex"
          selected-class="primary--text"
          column
        >
          <v-chip v-for="{icon, name} in examples" :key="name">
            <v-icon start>
              {{ icon }}
            </v-icon>
            {{ name }}
          </v-chip>
        </v-chip-group>
      </v-col>
    </v-row>

    <exampleDetails v-if="selectedDetailIndex !== undefined" class="mb-4" v-bind="examples[selectedDetailIndex]" />
  </div>
</template>

<script>

import { REPOSITORY_URL } from '@/constants'

export default {
  props: {
    examples: {
      type: Array,
      required: true
    }
  },
  data () {
    return {
      selectedDetailIndex: undefined,
      isSelectingUpload: false,
      selectedExample: {}
    }
  },
  watch: {
    selectedDetailIndex () {
      // !== undefined as it can be 0 so !! is not working
      this.loadExample(this.selectedDetailIndex !== undefined ? this.examples[this.selectedDetailIndex].file : null)
    },
    selectedExample () {
      this.$emit('selectedExample', this.selectedExample)
    }
  },
  mounted () {
    if (this.examples.length > 0) {
      this.selectedDetailIndex = 0
    }
  },
  methods: {
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
        // ToDo: make chip deselection work
        // $this.selectedDetailIndex = undefined
        console.log(event.target.result)
        // We're going to assume that the example we're trying to load starts
        // with a use case property (such as createCase, emsi, etc.) and that
        // it's the first and only property at the root level of the example.
        // TODO: instead of taking the first property and praying, we should extract
        // the name of the use case from somewhere and use it to properly access that
        // property in the parsed json object.
        const parsedJson = JSON.parse(event.target.result)
        $this.selectedExample = parsedJson[Object.keys(parsedJson)[0]]
      }
      const reader = new FileReader()
      reader.onload = onReaderLoad
      if (event.target.files[0]) {
        reader.readAsText(event.target.files[0])
      }
    },
    loadExample (exampleName) {
      if (exampleName) {
        fetch(REPOSITORY_URL + this.$config.public.modelBranch + '/src/main/resources/sample/examples/' + exampleName)
          .then(response => response.json())
          .then((data) => {
            this.selectedExample = data[Object.keys(data)[0]]
          })
      } else {
        this.selectedExample = {}
      }
    }
  }
}
</script>
