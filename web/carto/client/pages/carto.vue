<template>
  <div class="carto-container">
    <div id="map" style="height: 100%; width: 100%"></div>
  </div>
</template>

<script setup>
import { onMounted, watch } from 'vue';
import { useMainStore } from '~/store';
import { useAuthStore } from '@/store/auth';
import { useWebSocket } from '~/composables/useWebSocket';
import { useDAE } from '~/composables/useDAE';
import moment from 'moment';

const store = useMainStore();
const authStore = useAuthStore();
const { daeData, loadDAEList } = useDAE();
// eslint-disable-next-line @typescript-eslint/no-unused-vars
const { isConnected } = useWebSocket('Carto');

import 'leaflet/dist/leaflet.css';
import 'leaflet/dist/leaflet.js';

import * as L from 'leaflet';

import mapMarkerIcon from '~/assets/images/icons/map-marker.svg';
import daeIcon from '~/assets/images/icons/heart-flash.svg';
import smurIcon from '~/assets/images/icons/SMUR.png';

const DAE_MIN_ZOOM_LEVEL = 15;

const createDAEPopupContent = (dae) => {
  return `
    <div>
      <div>${dae.name || '-'}</div>
      <div>${dae.number || ''} ${dae.street || ''}</div>
      <div>${dae.postalCode || ''} ${dae.city || ''}</div>
      <div><span class="text-decoration-underline">Accès</span> : ${
        dae.accessibility || '-'
      }</div>
      <div><span class="text-decoration-underline">Détails</span> : ${
        dae.details || '-'
      }</div>
      <div><span class="text-decoration-underline">Disponibilité</span> : ${
        dae.disponibility || '-'
      }</div>
      <div class="my-1"><span class="font-italic">${dae.lastUpdate}</span></div>
    </div>`;
};

const createPositionPopupContent = (position) => {
  return `
    <div>
      <div>${position.resourceId || '-'}</div>
      <div><span class="text-decoration-underline">Disponibilité</span> : ${
        position.status || '-'
      }</div>
      <div><span class="text-decoration-underline">Statut</span> : ${
        position.engagedStatus || '-'
      }</div>
      <div class="my-1"><span class="font-italic">${moment(
        position.datetime
      ).format('DD/MM/yyyy hh:mm')}</span></div>
    </div>`;
};

let map;

async function initMap() {
  if (!process.client) return;
  map = L.default.map('map');

  L.default
    .tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap contributors',
    })
    .addTo(map);

  const entityCoordinates = [
    authStore.user.entity.lat,
    authStore.user.entity.long,
  ];

  // add entity marker
  L.default
    .marker(entityCoordinates, {
      icon: L.default.icon({
        iconUrl: mapMarkerIcon,
        className: 'entity-marker',
        iconSize: [30, 30],
        iconAnchor: [15, 30],
        popupAnchor: [0, -30],
      }),
    })
    .addTo(map)
    .bindPopup(authStore.user.entity.name);

  map.setView(entityCoordinates, authStore.user.entity.zoom);
}

onMounted(() => {
  initMap();

  const markers = {};
  const daeLayer = L.default.featureGroup();
  const positionLayer = L.default.featureGroup().addTo(map);

  // TODO
  loadDAEList(47.0, 47.816669, -1.25, 0.06667).then(() => {
    daeData.value.forEach((dae) => {
      L.default
        .marker([dae.lat, dae.long], {
          icon: L.default.icon({
            iconUrl: daeIcon,
            className: 'dae-marker',
            iconSize: [30, 30],
            iconAnchor: [15, 30],
            popupAnchor: [0, -30],
          }),
        })
        .bindPopup(createDAEPopupContent(dae))
        .addTo(daeLayer);
    });

    // displays markers from a minimum zoom level
    if (map.getZoom() >= DAE_MIN_ZOOM_LEVEL) {
      daeLayer.addTo(map);
    }
  });

  watch(
    () => store.positions,
    (newPositions) => {
      newPositions.forEach((position) => {
        let geoMarker;
        if (position.resourceId in markers) {
          // update existing marker
          geoMarker = markers[position.resourceId];
          geoMarker.setLatLng([position.coord[0].lat, position.coord[0].lon]);
          geoMarker.setIcon(
            L.default.icon({
              iconUrl: smurIcon,
              className: 'resource-disponible',
              iconSize: [30, 30],
              iconAnchor: [15, 30],
              popupAnchor: [0, -30],
            })
          );
          geoMarker.getPopup().setContent(createPositionPopupContent(position));
        } else {
          // create new marker
          geoMarker = L.default
            .marker([position.coord[0].lat, position.coord[0].lon], {
              icon: L.default.icon({
                iconUrl: smurIcon,
                className: 'resource-disponible',
                iconSize: [30, 30],
                iconAnchor: [15, 30],
                popupAnchor: [0, -30],
              }),
            })
            .addTo(positionLayer);
          geoMarker.bindPopup(createPositionPopupContent(position));
          markers[position.resourceId] = geoMarker; // store the marker for future updates
        }
      });
    },
    { immediate: true, deep: true }
  );

  // displays markers from a minimum zoom level
  map.on('zoomend', () => {
    const currentZoom = map.getZoom();
    if (currentZoom < DAE_MIN_ZOOM_LEVEL) {
      map.removeLayer(daeLayer);
    } else {
      if (!map.hasLayer(daeLayer)) {
        daeLayer.addTo(map);
      }
    }
  });
});
</script>

<script>
export default {
  name: 'Carto',
};
</script>

<style>
.carto-container {
  height: 100%;
  width: 100%;
  position: relative;
}

.dae-marker {
  filter: invert(32%) sepia(12%) saturate(5542%) hue-rotate(133deg) brightness(99%) contrast(101%);
}

.entity-marker {
  filter: invert(17%) sepia(68%) saturate(6298%) hue-rotate(354deg) brightness(91%) contrast(121%);
}
</style>
