const Environment = {
  BAS: "bac-a-sable",
  PREPROD: "pre-prod",
  PROD: "prod",
};

const CLIENTS_CONFIG_TABLE_ID = "table-annuaire-content";
const URL_RABBITMQ_ID = "url-rabbitmq";

const clientsConfigurations = {
  [Environment.BAS]: null,
  [Environment.PREPROD]: null,
  [Environment.PROD]: null,
};

const BASE_API_URL = "hub.esante.gouv.fr/annuaire/api";
const apiUrls = {
  [Environment.BAS]: `https://bac-a-sable.${BASE_API_URL}`,
  [Environment.PREPROD]: `https://pre-prod.${BASE_API_URL}`,
  [Environment.PROD]: `https://${BASE_API_URL}`,
};

const BASE_RABBITMQ_URL = "hub.esante.gouv.fr/rabbitmq";
const rabbitmqUrls = {
  [Environment.BAS]: `https://messaging.bac-a-sable.${BASE_RABBITMQ_URL}`,
  [Environment.PREPROD]: `https://messaging.pre-prod.${BASE_RABBITMQ_URL}`,
  [Environment.PROD]: `https://messaging.${BASE_RABBITMQ_URL}`,
};

const FILTERS_CONFIG = {
  samu: {
    id: "filter-samu",
    getValue: (item, value) => item.client_id === `fr.health.${value}`,
    getOptions: getSamu,
  },
  editor: {
    id: "filter-editor",
    getValue: (item, value) => item.editor === value,
    getOptions: getEditors,
  },
  vhost: {
    id: "filter-vhost",
    getValue: (item, value) => item.vhostList.includes(value),
    getOptions: getVhost,
  },
  perimeter: {
    id: "filter-perimeter",
    getValue: (item, value) => item[value] !== "",
    getOptions: getPerimeter,
  },
};

const perimeter = ["15-15", "15-smur", "15-nexsis", "15-gps"];
const colors = {
  [perimeter[0]]: "#9accdb",
  [perimeter[1]]: "#dbd19a",
  [perimeter[2]]: "#db9a9a",
  [perimeter[3]]: "#9adbb3",
};

const mddMap = {
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

const keyMap = {
  "P: 15-15": perimeter[0],
  "P: 15-smur": perimeter[1],
  "P: 15-nexsis": perimeter[2],
  "P: 15-gps": perimeter[3],
};

let selectedEnv = Environment.BAS;

function renameKeys(obj, keyMap) {
  const renamed = {};
  for (const key in obj) {
    const newKey = keyMap[key] || key;
    renamed[newKey] = obj[key];
  }
  return renamed;
}

window.addEventListener("load", async () => {
  // for (const env of Object.values(Environment)) {
  //   const clientsConfig = await fetchData(apiUrls[env]);
  //   clientsConfigurations[env] = clientsConfig.map((item) => {
  //     const renamedData = renameKeys(item, keyMap);
  //     const vhostList = constituteVhostList(renamedData);
  //     return {
  //       ...renamedData,
  //       vhostList: vhostList,
  //       isSelected: false,
  //     };
  //   });
  // }
  create_data_test();
  updateFiltersSelectOptions();
  renderUrl();
  renderClientsConfigTable(clientsConfigurations[selectedEnv]);
});

document.getElementById("env-buttons").addEventListener("click", (e) => {
  const btn = e.target.closest("button[data-env]");
  if (!btn) return;
  selectedEnv = btn.dataset.env;
  updateEnvButtonStyles();
  updateFiltersSelectOptions();
  renderUrl();
  renderClientsConfigTable(clientsConfigurations[selectedEnv]);
});

document.querySelectorAll("#div-filtres select").forEach((select) => {
  select.addEventListener("change", () => {
    renderClientsConfigTable(getCurrentFilteredClientsConfig());
  });
});

async function fetchData(url) {
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

function renderUrl() {
  const urlElement = document.getElementById(URL_RABBITMQ_ID);
  urlElement.innerHTML = rabbitmqUrls[selectedEnv];
}

function renderClientsConfigTable(clientsConfig) {
  const tableAnnuaireContent = document.getElementById(CLIENTS_CONFIG_TABLE_ID);
  tableAnnuaireContent.innerHTML = "";

  clientsConfig.forEach((item, index) => {
    const tr = document.createElement("tr");

    const tdCheckbox = document.createElement("td");
    tdCheckbox.style.textAlign = "center";
    const checkbox = document.createElement("input");
    checkbox.type = "checkbox";
    checkbox.dataset.index = index;
    checkbox.checked = !!item.isSelected;
    checkbox.onclick = function () {
      selection(this);
    };
    tdCheckbox.appendChild(checkbox);
    tr.appendChild(tdCheckbox);

    const tdClientID = document.createElement("td");
    tdClientID.textContent = item.client_id;
    tr.appendChild(tdClientID);

    const tdEditeur = document.createElement("td");
    tdEditeur.textContent = item.editor;
    tr.appendChild(tdEditeur);

    const tdVhost = document.createElement("td");
    tdVhost.style.display = "flex";
    tdVhost.style.flexWrap = "wrap";
    tdVhost.style.gap = "5px";
    item.vhostList.forEach((vhost) => {
      const vhostCard = createVhostCardElement(vhost);
      tdVhost.appendChild(vhostCard);
    });
    tr.appendChild(tdVhost);
    tableAnnuaireContent.appendChild(tr);
  });
}

function createVhostCardElement(vhost) {
  const vhostDiv = document.createElement("div");
  vhostDiv.style.border = "2px solid rgba(104, 105, 103, 0.2)";
  vhostDiv.style.borderRadius = "10px";
  vhostDiv.style.backgroundColor = colors[getPerimeterFromVhost(vhost)];
  vhostDiv.style.margin = "3px";
  vhostDiv.style.width = "30%";
  vhostDiv.style.textAlign = "center";
  vhostDiv.style.padding = "5px";

  const strong = document.createElement("strong");
  strong.textContent = vhost;
  vhostDiv.appendChild(strong);
  vhostDiv.appendChild(document.createElement("br"));
  const mdd = document.createElement("div");
  mdd.textContent = mddMap[vhost];
  vhostDiv.appendChild(mdd);

  return vhostDiv;
}

function getPerimeterFromVhost(vhost) {
  return vhost.split("_v")[0];
}

function updateEnvButtonStyles() {
  document.querySelectorAll(".btn-env").forEach((btn) => {
    if (btn.dataset.env === selectedEnv) {
      btn.classList.remove("btn--ghost");
      btn.classList.add("btn--plain");
    } else {
      btn.classList.remove("btn--plain");
      btn.classList.add("btn--ghost");
    }
  });
}

function updateFiltersSelectOptions() {
  for (const filter of Object.values(FILTERS_CONFIG)) {
    const select = document.getElementById(filter.id);
    select.length = 1; // garde seulement "Tous..."
    const options = filter.getOptions();
    options.sort().forEach((value) => {
      const option = document.createElement("option");
      option.value = value;
      option.textContent = value;
      select.appendChild(option);
    });
  }
}

function getCurrentFilteredClientsConfig() {
  return clientsConfigurations[selectedEnv].filter((item) =>
    Object.values(FILTERS_CONFIG).every((filter) => {
      const value = document.getElementById(filter.id).value;
      return value === "" || filter.getValue(item, value);
    }),
  );
}

function getEditors() {
  return [
    ...new Set(clientsConfigurations[selectedEnv].map((item) => item.editor)),
  ];
}
function getSamu() {
  return [
    ...new Set(
      clientsConfigurations[selectedEnv]
        .map((item) => item.client_id)
        .filter((item) => item.startsWith("fr.health.samu"))
        .map((item) => item.replace("fr.health.", "")),
    ),
  ];
}

function getVhost() {
  return [
    ...new Set(
      clientsConfigurations[selectedEnv].flatMap((item) => item.vhostList),
    ),
  ];
}

function getPerimeter() {
  return perimeter;
}

function constituteVhostList(data) {
  let vhostList = [];
  perimeter.forEach((p) => {
    let versions = data[p].split(",");
    versions.forEach((version) => {
      if (version !== "") vhostList.push(`${p}_v${version}`);
    });
  });
  return vhostList;
}

function create_data_test() {
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
      isSelected: false,
    };
  });
}
