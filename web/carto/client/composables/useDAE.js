// composables/useDAE.js
import { ref } from 'vue';
import { useRuntimeConfig } from '#app';
import moment from 'moment';

export function useDAE() {
  const config = useRuntimeConfig();

  const daeList = ref([]);
  const loading = ref(false);
  const error = ref(null);

  const transformDAE = (item) => ({
    name: item.c_nom,
    number: item.c_adr_num,
    street: item.c_adr_voie,
    postalCode: item.c_com_cp,
    city: item.c_com_nom,
    accessibility: item.c_acc,
    details: item.c_acc_complt,
    disponibility: `${item.c_disp_j.toString()} / ${item.c_disp_h.toString()}`,
    lat: item.c_lat_coor1,
    long: item.c_long_coor1,
    lastUpdate: moment(item.c__edit_datemaj).format('DD/MM/yyyy hh:mm'),
  });

  const loadDAEList = async (latMin, latMax, longMin, longMax) => {
    loading.value = true;
    error.value = null;
    try {
      let datas = [];
      let url =
        config.public.daeResourcesUrl +
        `&c_lat_coor1__greater=${latMin}&c_lat_coor1__less=${latMax}&c_long_coor1__greater=${longMin}&c_long_coor1__less=${longMax}`;

      while (url) {
        const response = await fetch(url);
        const result = await response.json();

        if (result.data?.length > 0) {
          datas = datas.concat(result.data);
        }

        url = result.links.next;
      }

      daeList.value = datas.map(transformDAE);
    } catch (err) {
      console.error('Error loading DAE data:', err);
      error.value = err;
      daeList.value = [];
    } finally {
      loading.value = false;
    }
  };

  return {
    daeData: daeList,
    loading,
    error,
    loadDAEList,
  };
}
