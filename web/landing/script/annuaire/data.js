import { CLIENT_ID_PREFIX, perimeter, perimeterLabels } from "./constants.js";

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

export function constituteLabel(data) {
  let label = "";
  let departmentNumber = "";
  const clientId = data.client_id;

  if (clientId.startsWith(CLIENT_ID_PREFIX.SAMU)) {
    label = "SAMU ";
    departmentNumber = clientId.slice(CLIENT_ID_PREFIX.SAMU.length);
  } else if (clientId.startsWith(CLIENT_ID_PREFIX.SNP)) {
    label = "SNP ";
    departmentNumber = clientId.slice(CLIENT_ID_PREFIX.SNP.length);
  }else if (clientId === "fr.health.si-cap") {
    return "SICAP";
  }
  if (departmentNumber.length === 3 && departmentNumber.endsWith("0")) {
    departmentNumber = departmentNumber.slice(0, -1);
  }

  return label + departmentNumber;
}

export function getDepartmentsInProd() {
  const clientIds = state.clientsConfigurations
    .map((item) => item.client_id)
    .filter(
      (client_id) =>
        client_id.startsWith(CLIENT_ID_PREFIX.SAMU) ||
        client_id.startsWith(CLIENT_ID_PREFIX.SNP),
    );

  const departmentNumbers = clientIds.map((client_id) => {
    let dep = client_id.replace(CLIENT_ID_PREFIX.SAMU, "");
    dep = dep.replace(CLIENT_ID_PREFIX.SNP, "");
    if (dep.length === 3 && dep.endsWith("0")) {
      dep = dep.slice(0, -1);
    }
    return dep;
  });

  return [...new Set(departmentNumbers)];
}

export function getActorsFromDepartment(numDep) {
  const clientIdSamu = `${CLIENT_ID_PREFIX.SAMU}${numDep.length === 3 ? numDep : numDep + "0"}`;
  const clientIdSnp = `${CLIENT_ID_PREFIX.SNP}${numDep.length === 3 ? numDep : numDep + "0"}`;
  return [
    ...new Set(
      state.clientsConfigurations.filter(
        (item) =>
          item.client_id === clientIdSamu || item.client_id === clientIdSnp,
      ),
    ),
  ];
}

export function sortClientConfig(clientsConfig) {
  return clientsConfig.sort((a, b) => {
    const editorA = (a.editor || "").toLowerCase();
    const editorB = (b.editor || "").toLowerCase();

    const editorCompare = editorA.localeCompare(editorB);
    if (editorCompare !== 0) return editorCompare;

    const clientIdA = (a.client_id || "").toLowerCase();
    const clientIdB = (b.client_id || "").toLowerCase();
    return clientIdA.localeCompare(clientIdB);
  });
}

export function getActorsInfo(clientConfig) {
  const activePerimeters = perimeter
    .filter((p) => clientConfig.perimeters?.[p])
    .map((p) => perimeterLabels[p]);
  return activePerimeters.join(", ");
}
