import { useMainStore } from '~/store';
import { REPOSITORY_URL } from '@/constants';
import { toast } from 'vue3-toastify';
import consola from 'consola';

export const loadSchemas = () => {
  const store = useMainStore();
  const source = store.selectedVhost.modelVersion;
  const messageTypesUrl =
    REPOSITORY_URL +
    source +
    '/src/main/resources/sample/examples/messagesList.json';
  const schemasUrl =
    REPOSITORY_URL + source + '/src/main/resources/json-schema/';

  return new Promise((resolve, reject) => {
    store
      .loadMessageTypes(messageTypesUrl)
      .then(() =>
        store
          .loadSchemas(schemasUrl)
          .then(() => {
            consola.info('messagesList.json and schemas loaded for ' + source);
            resolve();
          })
          .catch((reason) => {
            consola.error(reason);
            toast.error(
              "Erreur lors de l'acquisition des schémas de version " + source
            );
            reject(reason);
          })
      )
      .catch((reason) => {
        consola.error(reason);
        toast.error(
          "Erreur lors de l'acquisition de la liste des schémas de version " +
            source
        );
        reject(reason);
      });
  });
};
