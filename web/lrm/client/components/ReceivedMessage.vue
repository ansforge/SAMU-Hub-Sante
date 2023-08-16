<template>
  <div>
    <div class="mb-n4 ml-1">
      <v-badge
        v-if="showSentMessages"
        :icon="isOut(direction) ? 'mdi-upload' : 'mdi-download'"
      />
      <status-badge :direction="direction" :acked="acked" :distribution-id="body.distributionID" :class="{'ml-5': showSentMessages}" />
    </div>
    <div class="elevation-4 pt-8">
      <v-row class="mx-4">
        <span>
          <v-icon small left>mdi-email-fast</v-icon>{{ direction }} {{ isOut(direction) ? routingKey : body.senderID }}
          <br>
          <v-icon small left>mdi-timer</v-icon>{{ time }} → {{ isOut(direction) ? acked?.time : receivedTime }}
        </span>
        <v-spacer />
        <div v-if="getMessageType({body}) !== 'ack' && !isOut(direction)">
          <v-btn
            icon
            :color="acked ? 'accent' : 'primary'"
            @click="sendAck"
          >
            <v-icon>mdi-check-all</v-icon>
          </v-btn>
        </div>
        <v-btn
          icon
          color="primary"
          @click="showFullMessage = !showFullMessage"
        >
          <v-icon>{{ showFullMessage ? 'mdi-magnify-plus-outline' : 'mdi-magnify-minus-outline' }}</v-icon>
        </v-btn>
      </v-row>

      <json-viewer
        :value="showFullMessage ? body : body.content.contentObject.jsonContent.embeddedJsonContent.message"
        :expand-depth="1"
        :copyable="{copyText: 'Copier', copiedText: 'Copié !', timeout: 1000}"
        expanded
        theme="json-theme"
      />
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { DIRECTIONS } from '@/constants'
import mixinMessage from '~/plugins/mixinMessage'

export default {
  mixins: [mixinMessage],
  props: {
    direction: {
      type: String,
      required: true
    },
    routingKey: {
      type: String,
      required: true
    },
    time: {
      type: String,
      required: true
    },
    receivedTime: {
      type: String,
      default: null
    },
    body: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      DIRECTIONS,
      showFullMessage: false
    }
  },
  computed: {
    ...mapGetters(['messages']),
    acked () {
      // Within Ack messages, check if there is one matching the message
      return this.messages.filter(
        message => this.getMessageType(message) === 'ack'
      ).find(
        message => message.body.content.contentObject.jsonContent.embeddedJsonContent.message.ackDistributionId === this.body.distributionID
      )
    }
  },
  methods: {
    isOut (direction) {
      return direction === DIRECTIONS.OUT
    },
    sendAck () {
      try {
        const msg = this.buildMessage({ ackDistributionId: this.body.distributionID }, 'Ack')
        this.sendMessage(msg)
      } catch (error) {
        console.error("Erreur lors de l'envoi de l'acquittement", error)
      }
    }
  }
}
</script>
<style lang="scss">
// Ref.: https://github.com/chenfengjw163/vue-json-viewer/tree/master#theming
// values are default one from jv-light template
.json-theme {
  background: rgba(0, 0, 0, 0);
  white-space: nowrap;
  color: #525252;
  font-size: 14px;
  font-family: Consolas, Menlo, Courier, monospace;

  .jv-ellipsis {
    color: #999;
    background-color: #eee;
    display: inline-block;
    line-height: 0.9;
    font-size: 0.9em;
    padding: 0px 4px 2px 4px;
    border-radius: 3px;
    vertical-align: 2px;
    cursor: pointer;
    user-select: none;
  }
  .jv-button { color: #49b3ff }
  .jv-key { color: #111111 }
  .jv-item {
    &.jv-array { color: #111111 }
    &.jv-boolean { color: #fc1e70 }
    &.jv-function { color: #067bca }
    &.jv-number { color: #fc1e70 }
    &.jv-number-float { color: #fc1e70 }
    &.jv-number-integer { color: #fc1e70 }
    &.jv-object { color: #111111 }
    &.jv-undefined { color: #e08331 }
    &.jv-string {
      color: #42b983;
      word-break: break-word;
      white-space: normal;
    }
  }
  .jv-code {
    padding-bottom: 12px !important;
    .jv-toggle {
      &:before {
        padding: 0px 2px;
        border-radius: 2px;
      }
      &:hover {
        &:before {
          background: #eee;
        }
      }
    }
  }
}
</style>
