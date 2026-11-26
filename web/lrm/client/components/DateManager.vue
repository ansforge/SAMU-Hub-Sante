<template>
  <v-expansion-panels v-if="hasDates" class="date-manager">
    <v-expansion-panel>
      <v-expansion-panel-title>
        🕒 Modification globale des dates dans le formulaire
      </v-expansion-panel-title>
      <v-expansion-panel-text>
        <div class="date-manager__content">
          <v-alert
            border="start"
            color="info"
            density="compact"
            variant="tonal"
          >
            Toutes les dates du formulaire seront remplacées par cette valeur.
          </v-alert>

          <v-menu v-model="menuOpened" :close-on-content-click="false">
            <template #activator="{ props: activatorProps }">
              <v-text-field
                v-bind="activatorProps"
                :model-value="displayValue"
                label="Nouvelle date/heure"
                density="compact"
                readonly
              />
            </template>
            <v-sheet width="328" style="position: relative">
              <v-tabs v-model="tab" align-tabs="center">
                <v-tab value="date">
                  <v-icon icon="mdi-calendar" />
                </v-tab>
                <v-tab value="time">
                  <v-icon icon="mdi-clock-outline" />
                </v-tab>
              </v-tabs>
              <v-tabs-window v-model="tab">
                <v-tabs-window-item value="date">
                  <v-date-picker
                    hide-header
                    :model-value="dateObj"
                    @update:model-value="onDateSelected"
                  />
                </v-tabs-window-item>
                <v-tabs-window-item value="time">
                  <v-time-picker
                    hide-title
                    format="ampm"
                    density="compact"
                    class="mt-6"
                    :model-value="timeStr"
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

          <v-btn color="primary" @click="applyDate">
            Appliquer la date au formulaire
          </v-btn>
        </div>
      </v-expansion-panel-text>
    </v-expansion-panel>
  </v-expansion-panels>
</template>

<script setup>
import { computed, ref, toRefs } from 'vue';
import moment from 'moment';
import { useMainStore } from '~/store';
import {
  findPathsWithDates,
  setValuesAtPaths,
} from '~/composables/messageUtils';

const store = useMainStore();
const { currentMessage } = toRefs(store);

const menuOpened = ref(false);
const tab = ref('date');
const newDateTime = ref(moment().format());

const hasDates = computed(
  () => findPathsWithDates(currentMessage.value ?? {}).length > 0
);

const displayValue = computed(() =>
  moment(newDateTime.value).format('MMM D, YYYY, h:mm A')
);
const dateObj = computed(() => new Date(newDateTime.value));
const timeStr = computed(() => moment(newDateTime.value).format('HH:mm'));

function onDateSelected(date) {
  if (!date) return;
  const currentTime = moment(newDateTime.value).format('HH:mm:ss');
  newDateTime.value = moment(
    `${moment(date).format('YYYY-MM-DD')}T${currentTime}`
  ).format();
  tab.value = 'time';
}

function onTimeSelected(time) {
  if (!time) return;
  const datePart = moment(newDateTime.value).format('YYYY-MM-DD');
  newDateTime.value = moment(`${datePart}T${time}:00`).format();
}

function setNow() {
  newDateTime.value = moment().format();
}

function applyDate() {
  const paths = findPathsWithDates(currentMessage.value);
  currentMessage.value = setValuesAtPaths(
    currentMessage.value,
    paths,
    newDateTime.value
  );
}
</script>

<style scoped>
.date-manager__content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>
