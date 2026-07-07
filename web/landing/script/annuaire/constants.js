export const CLIENTS_CONFIG_TABLE_ID = "table-annuaire-content";
export const DIV_MAP_ID = "map";
export const DIV_INFO_DEPARTMENT_ID = "info-selected-department";
export const FILTER_IDS = {
  actor: "filter-actor",
  perimeter: "filter-perimeter",
};

export const BASE_API_URL = "hub.esante.gouv.fr/annuaire/api";
export const API_URL = "http://localhost:3000/annuaire";

export const RABBITMQ_URL = "amqps://messaging.hub.esante.gouv.fr:5671";

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
  "P: 15-nexsis": perimeter[1],
  "P: 15-smur": perimeter[2],
  "P: 15-gps": perimeter[3],
};
