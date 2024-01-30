<template>
  <div>
    <v-row>
      <v-col cols="auto" class="pr-0 mt-2">
        <v-btn
          fab
          outlined
          x-small
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
          active-class="primary--text"
          column
        >
          <v-chip v-for="{icon, name} in examples" :key="name">
            <v-icon left>
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

import { REPOSITORY_URL } from '@/constants'

<script>
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
        $this.selectedExample = JSON.parse(event.target.result)
      }
      const reader = new FileReader()
      reader.onload = onReaderLoad
      if (event.target.files[0]) {
        reader.readAsText(event.target.files[0])
      }
    },
    loadExample (exampleName) {
      if (exampleName) {
        fetch(REPOSITORY_URL + 'main/src/main/resources/sample/examples/' + exampleName)
          .then(response => response.json())
          .then((data) => {
            this.selectedExample = data
          })
      } else {
        this.selectedExample = {}
      }
    }
  }
}
</script>
