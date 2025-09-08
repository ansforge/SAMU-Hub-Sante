import { perimeter } from "./constants.js";
import { state } from "./data.js";

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

export function getActors() {
  return [...getSamu(), ...getSnp()];
}

export function getSamu() {
  return [
    ...new Set(
      state.clientsConfigurations
        .map((item) => item.client_id)
        .filter((item) => item.startsWith("fr.health.samu"))
        .map((item) => item.replace("fr.health.", "")),
    ),
  ];
}

export function getSnp() {
  return [
    ...new Set(
      state.clientsConfigurations
        .map((item) => item.client_id)
        .filter((item) => item.startsWith("fr.health.snp"))
        .map((item) => item.replace("fr.health.", "")),
    ),
  ];
}

export function getEditors() {
  return [...new Set(state.clientsConfigurations.map((item) => item.editor))];
}

export function getPerimeter() {
  return [...perimeter];
}

export function getCurrentFilteredClientsConfig() {
  return state.clientsConfigurations.filter((item) =>
    Object.values(FILTERS_CONFIG).every((filter) => {
      const value = document.getElementById(filter.id).value;
      return value === "" || filter.getValue(item, value);
    }),
  );
}
