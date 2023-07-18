<template>
  <div>
    <v-divider :inset="nested" />
    <v-card-title v-if="!nested">
      {{ requestInfos.name }}
    </v-card-title>
    <v-card-text v-else>
      {{ requestInfos.name }}
    </v-card-text>
    <v-form>
      <v-row>
        <v-col
          v-for="(propertyInfos, property) in requestInfos.properties"
          cols="12"
          :md="nested ? 12 : 6"
        >
          <v-combobox
            v-if="propertyInfos.type === 'string'"
            v-model="form[property]"
            :items="items[property]"
            :label="property"
            hide-details="auto"
            dense
          />
          <v-checkbox
            v-else-if="propertyInfos.type === 'boolean'"
            v-model="form[property]"
            :label="property"
            dense
          />
          <div v-else-if="propertyInfos.type === 'array'">
            <v-icon @click="pop(property)">
              mdi-minus
            </v-icon>
            {{ form[property]?.length ?? 0 }} {{ property }}
            <v-icon @click="push(property)">
              mdi-plus
            </v-icon>
            <!-- i - 1 as v-for starts from 1... -->
            <RequestForm
              v-for="i in form[property]?.length ?? 0"
              :key="i"
              v-model="form[property][i - 1]"
              :swagger="swagger"
              :request-infos="{
                name: property + '[' + i + ']',
                properties: { [property]: propertyInfos.items }
              }"
              is-item
              :items="items[property]"
              nested
            />
          </div>
          <div v-else-if="'$ref' in propertyInfos">
            <RequestForm
              v-model="form[property]"
              :swagger="swagger"
              :request-infos="{
                ...(!isItem && {name: property}),
                ...swagger.definitions[propertyInfos.$ref.split('/').at(-1)]
              }"
              :items="items"
              nested
            />
          </div>
          <v-card-text v-else>
            Missing {{ property }} : {{ propertyInfos }}
          </v-card-text>
        </v-col>
      </v-row>
      <v-card-actions v-if="!nested">
        <v-spacer />
        <v-btn color="primary" @click="$emit('submit')">
          <v-icon left>
            mdi-send
          </v-icon>
          {{ requestInfos.actionName }}
        </v-btn>
      </v-card-actions>
    </v-form>
  </div>
</template>

<script>
export default {
  props: {
    value: {
      type: Object,
      default: () => ({})
    },
    swagger: {
      type: Object,
      required: true
    },
    requestInfos: {
      type: Object,
      required: true
    },
    items: {
      type: Object,
      default: () => ({})
    },
    isItem: {
      // If it should be stored as 'property => { data }' or just as 'data' (when item of array)
      type: Boolean,
      default: false
    },
    nested: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      // used for array properties
      counts: {},
      // Passed through v-model
      form: this.value
    }
  },
  watch: {
    form () {
      // Flatten data to convert '{ propertyName: { data } }' to '{ data }' as there is no need for propertyName in array
      if (this.isItem) {
        console.assert(Object.keys(this.form).length === 1)
        this.$emit('input', this.form[Object.keys(this.form)[0]])
      } else {
        this.$emit('input', this.form)
      }
    }
  },
  methods: {
    // Needed to make sure array is setup -> if not, create empty reactive array
    push (property) {
      if (!this.form[property]) {
        this.$set(this.form, property, [])
      }
      this.form[property].push({})
    },
    pop (property) {
      if (!this.form[property]) {
        this.$set(this.form, property, [])
      }
      this.form[property].pop()
    }
  }
}
</script>
