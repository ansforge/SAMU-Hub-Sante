import {
  keyMap,
  perimeter,
  clientsConfigurations,
  Environment,
  perimeterInVhost,
} from "./constants.js";
import { getSelectedEnv } from "./env.js";

export async function fetchData(url) {
  try {
    const response = await fetch(url);
    if (!response.ok) {
      throw new Error(`Erreur HTTP : ${response.status}`);
    }
    return await response.json();
  } catch (e) {
    console.error(
      `Erreur lors de la récupération des données depuis ${url} :`,
      e,
    );
    return null;
  }
}

export function renameKeys(obj, keyMap) {
  const renamed = {};
  for (const key in obj) {
    const newKey = keyMap[key] || key;
    renamed[newKey] = obj[key];
  }
  return renamed;
}

export function constituteVhostList(data) {
  let vhostList = [];
  perimeter.forEach((p) => {
    let versions = data[p].split(",");
    versions.forEach((version) => {
      if (version !== "") vhostList.push(`${perimeterInVhost[p]}_v${version}`);
    });
  });
  return vhostList;
}

export function getClientConfigByDepartment(numDep) {
  return clientsConfigurations[Environment.PROD].find(
    (item) =>
      item.client_id === `fr.health.samu${numDep}0` ||
      item.client_id === `fr.health.samu${numDep}`,
  );
}

export function getSelectedClientsConfig() {
  return clientsConfigurations[getSelectedEnv()].filter(
    (item) => item.isSelected,
  );
}

export function resetSelectedClientsConfig() {
  clientsConfigurations[getSelectedEnv()].forEach((item) => {
    item.isSelected = false;
  });
}

export function getDepartmentInProd() {
  return [
    ...new Set(
      clientsConfigurations[Environment.PROD]
        .map((item) => item.client_id)
        .filter((client_id) => client_id.startsWith("fr.health.samu"))
        .map((client_id) => {
          let dep = client_id.replace("fr.health.samu", "");
          if (dep.length === 3 && dep.endsWith("0")) {
            dep = dep.slice(0, -1);
          }
          return dep;
        }),
    ),
  ];
}
