export const CLIENTS_CONFIG_TABLE_ID = "table-annuaire-content";
export const DIV_MAP_ID = "map";
export const DIV_INFO_DEPARTMENT_ID = "info-selected-department";
export const FILTER_IDS = {
  actor: "filter-actor",
  perimeter: "filter-perimeter",
};

// Détecte automatiquement l'environnement à partir du nom de domaine
function detectEnv() {
  const host = window.location.hostname;
  if (host.includes("localhost") || host.includes("127.0.0.1")) return "dev";
  if (host.includes("bac-a-sable")) return "bac-a-sable";
  if (host.includes("integration")) return "integration";
  if (host.includes("qualification")) return "qualification";
  if (host.includes("pre-prod")) return "pre-prod";
  return "prod";
}

const ENV = detectEnv();

const BASE_API_URLS = {
  dev: "http://localhost:3000/annuaire",
  "bac-a-sable": "https://bac-a-sable.hub.esante.gouv.fr/annuaire/api",
  integration: "https://integration.hub.esante.gouv.fr/annuaire/api",
  qualification: "https://qualification.hub.esante.gouv.fr/annuaire/api",
  "pre-prod": "https://pre-prod.hub.esante.gouv.fr/annuaire/api",
  prod: "https://hub.esante.gouv.fr/annuaire/api",
};

export const BASE_API_URL = BASE_API_URLS[ENV];
export const API_URL = ENV === "dev" ? BASE_API_URL : `${BASE_API_URL}/clients`;

export const CLIENT_ID_PREFIX = {
  SAMU: "fr.health.samu",
  SNP: "fr.health.snp",
};

export const perimeter = [
  "15-15", "15-nexsis", "15-smur", "15-gps",
  "15-cap", "15-cnr114", "15-portail",
];

export const perimeterLabels = {
  "15-15": "15-15", "15-nexsis": "15-NexSIS", "15-smur": "15-SMUR/RPIS",
  "15-gps": "15-GPS", "15-cap": "15-CAP", "15-cnr114": "15-CNR114",
  "15-portail": "15-Portail",
};

export const colors = {
  [perimeter[0]]: "#369AEE", //(var(--information))
  [perimeter[1]]: "#E11414", //(var(--red))
  [perimeter[2]]: "#FAE832", //(var(--yellow))
  [perimeter[3]]: "#9AB938", //(var(--success))
};
export const perimeterInVhost = {
  [perimeter[0]]: "15-15",
  [perimeter[1]]: "15-nexsis",
  [perimeter[2]]: "15-smur",
  [perimeter[3]]: "15-gps",
};

export const perimeterPdfLinks = {
  "15-15": "/resources/Liens/Lien_15-15.pdf",
  "15-nexsis": "/resources/Liens/Lien_15-NexSiS.pdf",
  "15-smur": "/resources/Liens/Lien_15-SMUR.pdf",
  "15-cap": "/resources/Liens/Lien_15-CAP.pdf",
};
