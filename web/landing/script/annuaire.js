const Environment = Object.freeze({
  BAS: 'bac-a-sable',
  PREPROD: 'pre-prod',
  PROD: 'prod',
});

const clientsConfigurations = {
  [Environment.BAS]: null,
  [Environment.PREPROD]: null,
  [Environment.PROD]: null,
};

const BASE_API_URL = "hub.esante.gouv.fr/annuaire/api";

const apiUrls = {
  [Environment.BAS]: `https://bac-a-sable.${BASE_API_URL}`,
  [Environment.PREPROD]: `https://pre-prod.${BASE_API_URL}`,
  [Environment.PROD]: `https://${BASE_API_URL}`,
};

window.addEventListener("load", async () => {
  for (const env of Object.values(Environment)) {
    clientsConfigurations[env] = await fetchData(apiUrls[env]);
  }
});

async function fetchData(url) {
  try {
    const response = await fetch(url);
    if (!response.ok) {
      throw new Error(`Erreur HTTP : ${response.status}`);
    }
    return await response.json();
  } catch (e) {
    console.error(`Erreur lors de la récupération des données depuis ${url} :`, e);
    return null;
  }
}