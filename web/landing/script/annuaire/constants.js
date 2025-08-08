export const CLIENTS_CONFIG_TABLE_ID = "table-annuaire-content";
export const URL_RABBITMQ_ID = "url-rabbitmq";
export const RECAP_OPEN_BTN_ID = "btn-open-recap";
export const RECAP_CONTAINER_ID = "recap-container";
export const RECAP_CONTENT_ID = "recap-content";
export const RECAP_CLOSE_BTN_ID = "btn-close-recap";
export const DIV_MAP_ID = "div-map"
export const DIV_INFO_DEPARTMENT_ID = "info-selected-department";

export const Environment = {
  BAS: "bac-a-sable",
  PREPROD: "pre-prod",
  PROD: "prod",
};

export const clientsConfigurations = {
  [Environment.BAS]: null,
  [Environment.PREPROD]: null,
  [Environment.PROD]: null,
};

export const BASE_API_URL = "hub.esante.gouv.fr/annuaire/api";
export const apiUrls = {
  [Environment.BAS]: `https://bac-a-sable.${BASE_API_URL}`,
  [Environment.PREPROD]: `https://pre-prod.${BASE_API_URL}`,
  [Environment.PROD]: `https://${BASE_API_URL}`,
};

export const BASE_RABBITMQ_URL = "hub.esante.gouv.fr/rabbitmq";
export const rabbitmqUrls = {
  [Environment.BAS]: `https://messaging.bac-a-sable.${BASE_RABBITMQ_URL}`,
  [Environment.PREPROD]: `https://messaging.pre-prod.${BASE_RABBITMQ_URL}`,
  [Environment.PROD]: `https://messaging.${BASE_RABBITMQ_URL}`,
};

export const perimeter = ["15-15", "15-smur", "15-nexsis", "15-gps"];
export const colors = {
  [perimeter[0]]: "#9accdb",
  [perimeter[1]]: "#dbd19a",
  [perimeter[2]]: "#db9a9a",
  [perimeter[3]]: "#9adbb3",
};

export const mddMap = {
  "15-15_v1.5": "1.0",
  "15-15_v2.0": "2.0",
  "15-15_v2.1": "3.0",
  "15-smur_v1.4": "1.0",
  "15-smur_v1.5": "1.0",
  "15-smur_v1.6": "2.0",
  "15-smur_v1.7": "3.0",
  "15-nexsis_v1.8": "1.0",
  "15-nexsis_v1.9": "2.0",
  "15-nexsis_v1.9.1": "3.0",
  "15-gps_v1.0": "1.0",
  "15-gps_v1.1": "1.0",
  "15-gps_v1.2": "2.0",
  "15-gps_v1.3": "3.0",
};

export const keyMap = {
  "P: 15-15": perimeter[0],
  "P: 15-smur": perimeter[1],
  "P: 15-nexsis": perimeter[2],
  "P: 15-gps": perimeter[3],
};
