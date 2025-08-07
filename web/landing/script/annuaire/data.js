import { keyMap, perimeter, clientsConfigurations, Environment } from "./constants.js";
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
      if (version !== "") vhostList.push(`${p}_v${version}`);
    });
  });
  return vhostList;
}

export function getSelectedClientsConfig() {
    return clientsConfigurations[getSelectedEnv()].filter((item) => item.isSelected);
}

export function resetSelectedClientsConfig() {
    clientsConfigurations[getSelectedEnv()].forEach((item) => {
        item.isSelected = false;
    });
}


export function create_data_test() {
  const data = [
    {
      "P: 15-15": "1.5,2.0,2.1",
      "P: 15-gps": "1.3",
      "P: 15-nexsis": "1.9",
      "P: 15-smur": "1.6,1.7",
      client_id: "fr.health.lrm",
      editor: "LRM",
    },
    {
      "P: 15-15": "1.5,2.0,2.1",
      "P: 15-gps": "1.3",
      "P: 15-nexsis": "1.9",
      "P: 15-smur": "1.6,1.7",
      client_id: "fr.health.test.samuA",
      editor: "LRM",
    },
    {
      "P: 15-15": "1.5,2.0,2.1",
      "P: 15-gps": "1.3",
      "P: 15-nexsis": "1.9",
      "P: 15-smur": "1.6,1.7",
      client_id: "fr.health.test.samuB",
      editor: "LRM",
    },
    {
      "P: 15-15": "1.5,2.0,2.1",
      "P: 15-gps": "1.3",
      "P: 15-nexsis": "1.9",
      "P: 15-smur": "1.6,1.7",
      client_id: "fr.health.test.samuC",
      editor: "LRM",
    },
    {
      "P: 15-15": "1.5,2.0,2.1",
      "P: 15-gps": "1.3",
      "P: 15-nexsis": "1.9",
      "P: 15-smur": "1.6,1.7",
      client_id: "fr.health.test.samuRA",
      editor: "LRM",
    },
    {
      "P: 15-15": "1.5,2.0,2.1",
      "P: 15-gps": "1.3",
      "P: 15-nexsis": "1.9",
      "P: 15-smur": "1.6,1.7",
      client_id: "fr.health.samu180",
      editor: "LRM",
    },
    {
      "P: 15-15": "1.5,2.0,2.1",
      "P: 15-gps": "1.3",
      "P: 15-nexsis": "1.9",
      "P: 15-smur": "",
      client_id: "fr.health.samu950",
      editor: "LRM",
    },
    {
      "P: 15-15": "1.5",
      "P: 15-gps": "",
      "P: 15-nexsis": "",
      "P: 15-smur": "",
      client_id: "fr.health.test.samuv1",
      editor: "LRM",
    },
    {
      "P: 15-15": "2.0",
      "P: 15-gps": "",
      "P: 15-nexsis": "1.6",
      "P: 15-smur": "",
      client_id: "fr.health.test.samuv2",
      editor: "LRM",
    },
    {
      "P: 15-15": "2.1",
      "P: 15-gps": "1.3",
      "P: 15-nexsis": "1.7",
      "P: 15-smur": "1.6,1.7",
      client_id: "fr.health.test.samuv3",
      editor: "LRM",
    },
    {
      "P: 15-15": "",
      "P: 15-gps": "",
      "P: 15-nexsis": "",
      "P: 15-smur": "1.6,1.7",
      client_id: "fr.cisu.sdisY",
      editor: "LRM",
    },
    {
      "P: 15-15": "",
      "P: 15-gps": "",
      "P: 15-nexsis": "",
      "P: 15-smur": "1.6,1.7",
      client_id: "fr.fire.nexsis.sdisZ",
      editor: "LRM",
    },
    {
      "P: 15-15": "",
      "P: 15-gps": "1.3",
      "P: 15-nexsis": "",
      "P: 15-smur": "",
      client_id: "fr.health.carto",
      editor: "carto",
    },
    {
      "P: 15-15": "",
      "P: 15-gps": "",
      "P: 15-nexsis": "",
      "P: 15-smur": "1.6,1.7",
      client_id: "fr.health.test.samuv3-direct-cisu",
      editor: "LRM",
    },
  ];
  clientsConfigurations[Environment.BAS] = data.map((item) => {
    const renamedData = renameKeys(item, keyMap);
    const vhostList = constituteVhostList(renamedData);
    return {
      ...renamedData,
      vhostList: vhostList,
      isSelected: false,
    };
  });
  clientsConfigurations[Environment.PREPROD] = data.map((item) => {
    const renamedData = renameKeys(item, keyMap);
    const vhostList = constituteVhostList(renamedData);
    return {
      ...renamedData,
      vhostList: vhostList,
      isSelected: false,
    };
  });
  clientsConfigurations[Environment.PROD] = data.map((item) => {
    const renamedData = renameKeys(item, keyMap);
    const vhostList = constituteVhostList(renamedData);
    return {
      ...renamedData,
      vhostList: vhostList,
      isSelected: true,
    };
  });
}
