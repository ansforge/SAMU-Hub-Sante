import { perimeter } from "./constants.js";
import { state } from "./data.js";

export const FILTERS_CONFIG = {
  samu: {
    id: "filter-actor",
    getValue: (item, value) => item.label === value,
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
  return [
    ...new Set(
      state.clientsConfigurations
        .map((item) => item.label)
        .filter((label) => label !== ""),
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
