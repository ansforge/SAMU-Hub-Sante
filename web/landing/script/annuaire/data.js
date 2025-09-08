import { perimeter, perimeterInVhost } from "./constants.js";

export const state = { clientsConfigurations: [] };

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
  return state.clientsConfigurations.find(
    (item) =>
      item.client_id === `fr.health.samu${numDep}0` ||
      item.client_id === `fr.health.samu${numDep}`,
  );
}

export function getDepartmentInProd() {
  return [
    ...new Set(
      state.clientsConfigurations
        .map((item) => item.client_id)
        .filter(
          (client_id) =>
            client_id.startsWith("fr.health.samu") ||
            client_id.startsWith("fr.health.snp"),
        )
        .map((client_id) => {
          let dep = client_id.replace("fr.health.samu", "");
          dep = dep.replace("fr.health.snp", "");
          if (dep.length === 3 && dep.endsWith("0")) {
            dep = dep.slice(0, -1);
          }
          return dep;
        }),
    ),
  ];
}

export function getActorsFromDepartment(numDep) {
  return [
    ...new Set(
      state.clientsConfigurations.filter(
        (item) =>
          item.client_id === `fr.health.samu${numDep}0` ||
          item.client_id === `fr.health.samu${numDep}` ||
          item.client_id === `fr.health.snp${numDep}0` ||
          item.client_id === `fr.health.snp${numDep}`,
      ),
    ),
  ];
}
