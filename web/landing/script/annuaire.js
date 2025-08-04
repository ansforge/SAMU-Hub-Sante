const Environment = {
  BAS: "bac-a-sable",
  PREPROD: "pre-prod",
  PROD: "prod",
};

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

const perimeter = ["P1515", "P15smur", "P15nexsis", "P15gps"];
const colors = {
  P1515: "#9accdb",
  P15smur: "#dbd19a",
  P15nexsis: "#db9a9a",
  P15gps: "#9adbb3",
};

const mddMap = {
  [perimeter[0]]: {
    1.5: "1",
    "2.0": "2",
    2.1: "3",
  },
  [perimeter[1]]: {
    1.4: "1",
    1.5: "1",
    1.6: "2",
    1.7: "3",
  },
  [perimeter[2]]: {
    1.8: "1",
    2.9: "2",
    "1.9.1": "3",
  },
  [perimeter[3]]: {
    "1.0": "1",
    1.1: "1",
    1.2: "2",
    1.3: "3",
  },
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
  //   const data = await fetchData(apiUrls[env]);
  //   clientsConfigurations[env] = data.map((item) => {
  //     const renamedData = renameKeys(item, keyMap);
  //     return {
  //       ...renamedData,
  //       isSelected: false,
  //     };
  //   });
  // }
  create_data_test();
  updateFilters();
  renderUrl();
  renderTable(clientsConfigurations[Environment.BAS]);
});

document.getElementById("env-buttons").addEventListener("click", (e) => {
  const btn = e.target.closest("button[data-env]");
  if (!btn) return;
  selectedEnv = btn.dataset.env;
  updateEnvButtonStyles();
  updateFilters();
  renderUrl();
  renderTable(clientsConfigurations[selectedEnv]);
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
  const urlElement = document.getElementById("url-rabbitmq");
  urlElement.innerHTML = rabbitmqUrls[selectedEnv];
}

function renderTable(data) {
  const tableAnnuaireContent = document.getElementById(
    "table-annuaire-content",
  );
  tableAnnuaireContent.innerHTML = "";

  data.forEach((item, index) => {
    const tr = document.createElement("tr");

    // Checkbox
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

    const tdPerimetre = document.createElement("td");

    tr.appendChild(tdPerimetre);
    tableAnnuaireContent.appendChild(tr);
  });
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

function updateFilters() {
  updateSelectOptions("filter-editor", getEditors());
  updateSelectOptions("filter-samu", getSamu());
  updateSelectOptions("filter-vhost", getVhost());
  updateSelectOptions("filter-perimeter", perimeter);
}

function updateSelectOptions(selectId, options) {
  const select = document.getElementById(selectId);
  // Supprime les anciennes options sauf la première (le "Tous...")
  select.length = 1;

  options.sort().forEach((value) => {
    const option = document.createElement("option");
    option.value = value;
    option.textContent = value;
    select.appendChild(option);
  });
}

function getEditors() {
  return [
    ...new Set(clientsConfigurations[selectedEnv].map((item) => item.editor)),
  ];
}
function getSamu() {
  return [];
}
function getVhost() {
  return [];
}

function create_data_test() {
  const data = [
    {
      "P: 15-15": "1.5,2.0,2.1",
      "P: 15-gps": "1.3",
      "P: 15-nexsis": "1.6,1.7",
      "P: 15-smur": "1.9",
      client_id: "fr.health.lrm",
      editor: "LRM",
    },
    {
      "P: 15-15": "1.5,2.0,2.1",
      "P: 15-gps": "1.3",
      "P: 15-nexsis": "1.6,1.7",
      "P: 15-smur": "1.9",
      client_id: "fr.health.test.samuA",
      editor: "LRM",
    },
    {
      "P: 15-15": "1.5,2.0,2.1",
      "P: 15-gps": "1.3",
      "P: 15-nexsis": "1.6,1.7",
      "P: 15-smur": "1.9",
      client_id: "fr.health.test.samuB",
      editor: "LRM",
    },
    {
      "P: 15-15": "1.5,2.0,2.1",
      "P: 15-gps": "1.3",
      "P: 15-nexsis": "1.6,1.7",
      "P: 15-smur": "1.9",
      client_id: "fr.health.test.samuC",
      editor: "LRM",
    },
    {
      "P: 15-15": "1.5,2.0,2.1",
      "P: 15-gps": "1.3",
      "P: 15-nexsis": "1.6,1.7",
      "P: 15-smur": "1.9",
      client_id: "fr.health.test.samuRA",
      editor: "LRM",
    },
    {
      "P: 15-15": "1.5,2.0,2.1",
      "P: 15-gps": "1.3",
      "P: 15-nexsis": "1.6,1.7",
      "P: 15-smur": "1.9",
      client_id: "fr.health.test.samuRB",
      editor: "LRM",
    },
    {
      "P: 15-15": "1.5,2.0,2.1",
      "P: 15-gps": "1.3",
      "P: 15-nexsis": "1.6,1.7",
      "P: 15-smur": "1.9",
      client_id: "fr.health.test.samuRC",
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
      "P: 15-smur": "1.9",
      client_id: "fr.health.test.samuv3",
      editor: "LRM",
    },
    {
      "P: 15-15": "",
      "P: 15-gps": "",
      "P: 15-nexsis": "",
      "P: 15-smur": "1.9",
      client_id: "fr.cisu.sdisY",
      editor: "LRM",
    },
    {
      "P: 15-15": "",
      "P: 15-gps": "",
      "P: 15-nexsis": "",
      "P: 15-smur": "1.9",
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
      "P: 15-smur": "1.9",
      client_id: "fr.health.test.samuv3-direct-cisu",
      editor: "LRM",
    },
  ];
  clientsConfigurations[Environment.BAS] = data.map((item) => {
      const renamedData = renameKeys(item, keyMap);
      return {
        ...renamedData,
        isSelected: false,
      };
    });
  clientsConfigurations[Environment.PREPROD] = data.map((item) => {
      const renamedData = renameKeys(item, keyMap);
      return {
        ...renamedData,
        isSelected: false,
      };
    });
  clientsConfigurations[Environment.PROD] = data.map((item) => {
      const renamedData = renameKeys(item, keyMap);
      return {
        ...renamedData,
        isSelected: true,
      };
    });

}
