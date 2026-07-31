<template>
  <v-menu v-model="menuOpened" :close-on-content-click="false">
    <template #activator="{ props: activatorProps }">
      <v-text-field
        v-bind="activatorProps"
        :model-value="displayValue"
        :label="modelValue.layout?.label"
        readonly
        prepend-inner-icon="mdi-calendar"
      />
    </template>

    <v-sheet width="328" style="position: relative">
      <v-tabs v-model="tab" align-tabs="center">
        <v-tab value="date">
          <v-icon icon="mdi-calendar" />
        </v-tab>
        <v-tab value="time" :disabled="!modelValue.data">
          <v-icon icon="mdi-clock-outline" />
        </v-tab>
      </v-tabs>
      <v-tabs-window v-model="tab">
        <v-tabs-window-item value="date">
          <v-date-picker
            hide-header
            :model-value="localDateObj"
            @update:model-value="onDateSelected"
          />
        </v-tabs-window-item>
        <v-tabs-window-item value="time">
          <v-time-picker
            hide-title
            format="ampm"
            density="compact"
            class="mt-8"
            :model-value="localTimeStr"
            @update:model-value="onTimeSelected"
          />
        </v-tabs-window-item>
      </v-tabs-window>
      <v-btn
        v-if="tab === 'date'"
        size="small"
        variant="tonal"
        color="primary"
        prepend-icon="mdi-clock-check-outline"
        style="
          position: absolute;
          bottom: 10px;
          left: 50%;
          transform: translateX(-50%);
        "
        @click="setNow"
      >
        Aujourd'hui
      </v-btn>
    </v-sheet>
  </v-menu>
</template>

<script setup>
import { ref, computed } from 'vue';
import moment from 'moment';

const props = defineProps({
  modelValue: {
    type: Object,
    required: true,
  },
  statefulLayout: {
    type: Object,
    required: true,
  },
});

const menuOpened = ref(false);
const tab = ref('date');

const displayValue = computed(() => {
  if (!props.modelValue.data) return '';
  return moment(props.modelValue.data).format('MMM D, YYYY, h:mm A');
});

const localDateObj = computed(() =>
  props.modelValue.data ? new Date(props.modelValue.data) : null
);

const localTimeStr = computed(() =>
  props.modelValue.data ? moment(props.modelValue.data).format('HH:mm') : null
);

function onDateSelected(date) {
  if (!date) return;
  const currentTime = props.modelValue.data
    ? moment(props.modelValue.data).format('HH:mm:ss')
    : moment().format('HH:mm:ss');
  const newDateTime = `${moment(date).format('YYYY-MM-DD')}T${currentTime}`;
  props.statefulLayout.input(props.modelValue, moment(newDateTime).format());
  tab.value = 'time';
}

function onTimeSelected(time) {
  if (!time) return;
  const datePart = props.modelValue.data
    ? moment(props.modelValue.data).format('YYYY-MM-DD')
    : moment().format('YYYY-MM-DD');
  props.statefulLayout.input(
    props.modelValue,
    moment(`${datePart}T${time}:00`).format()
  );
}

function setNow() {
  props.statefulLayout.input(props.modelValue, moment().format());
  menuOpened.value = false;
}
</script>
