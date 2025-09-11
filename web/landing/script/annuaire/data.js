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

export function constituteLabel(data) {
  let label = "";
  let departmentNumber = "";
  const clientId = data.client_id;

  if (clientId.startsWith("fr.health.samu")) {
    label = "SAMU ";
    departmentNumber = clientId.slice("fr.health.samu".length);
  } else if (clientId.startsWith("fr.health.snp")) {
    label = "SNP ";
    departmentNumber = clientId.slice("fr.health.snp".length);
  }

  if (departmentNumber.length === 3 && departmentNumber.endsWith("0")) {
    departmentNumber = departmentNumber.slice(0, -1);
  }

  return label + departmentNumber;
}

export function getDepartmentInProd() {
  const clientIds = state.clientsConfigurations
    .map((item) => item.client_id)
    .filter(
      (client_id) =>
        client_id.startsWith("fr.health.samu") ||
        client_id.startsWith("fr.health.snp"),
    );

  const departmentNumbers = clientIds.map((client_id) => {
    let dep = client_id.replace("fr.health.samu", "");
    dep = dep.replace("fr.health.snp", "");
    if (dep.length === 3 && dep.endsWith("0")) {
      dep = dep.slice(0, -1);
    }
    return dep;
  });

  return [...new Set(departmentNumbers)];
}

export function getActorsFromDepartment(numDep) {
  const clientIdSamu = `fr.health.samu${numDep.length === 3 ? numDep : numDep + "0"}`;
  const clientIdSnp = `fr.health.snp${numDep.length === 3 ? numDep : numDep + "0"}`;
  return [
    ...new Set(
      state.clientsConfigurations.filter(
        (item) =>
          item.client_id === clientIdSamu || item.client_id === clientIdSnp,
      ),
    ),
  ];
}
