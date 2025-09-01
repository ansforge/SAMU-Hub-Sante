import { perimeter, clientsConfigurations } from "./constants.js";
import { getSelectedEnv } from "./env.js";

export const FILTERS_CONFIG = {
  samu: {
    id: "filter-actor",
    getValue: (item, value) => item.client_id === `fr.health.${value}`,
    getOptions: getActors,
  },
  editor: {
    id: "filter-editor",
    getValue: (item, value) => item.editor === value,
    getOptions: getEditors,
  },
  perimeter: {
    id: "filter-perimeter",
    getValue: (item, value) => item[value] !== "",
    getOptions: getPerimeter,
  },
};

export function getActors(){
  return [...getSamu()  ]
}

export function getSamu() {
  return [
    ...new Set(
      clientsConfigurations[getSelectedEnv()]
        .map((item) => item.client_id)
        .filter((item) => item.startsWith("fr.health.samu"))
        .map((item) => item.replace("fr.health.", "")),
    ),
  ];
}

export function getEditors() {
  return [
    ...new Set(
      clientsConfigurations[getSelectedEnv()].map((item) => item.editor),
    ),
  ];
}

export function getVhost() {
  return [
    ...new Set(
      clientsConfigurations[getSelectedEnv()].flatMap((item) => item.vhostList),
    ),
  ];
}

export function getPerimeter() {
  return perimeter;
}

export function getCurrentFilteredClientsConfig() {
  return clientsConfigurations[getSelectedEnv()].filter((item) =>
    Object.values(FILTERS_CONFIG).every((filter) => {
      const value = document.getElementById(filter.id).value;
      return value === "" || filter.getValue(item, value);
    }),
  );
}
