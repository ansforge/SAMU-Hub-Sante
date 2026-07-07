import { perimeter, FILTER_IDS } from "./constants.js";
import { state } from "./data.js";

export const FILTERS_CONFIG = {
  samu: {
    id: FILTER_IDS.actor,
    getValue: (item, value) => item.label === value,
    getOptions: getActors,
  },
  perimeter: {
    id: FILTER_IDS.perimeter,
    getValue: (item, value) => !!item.perimeters?.[value],
    getOptions: getPerimeter,
  },
};

export function getActors() {
  return [
    ...new Set(
      state.clientsConfigurations
        .map((item) => item.label)
        .filter((label) => label !== ""),
    ),
  ];
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
