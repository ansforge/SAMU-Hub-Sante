import { URL_RABBITMQ_ID, CLIENTS_CONFIG_TABLE_ID, mddMap, rabbitmqUrls, colors } from "./constants.js";
import { FILTERS_CONFIG } from "./filters.js";

export function renderUrl(selectedEnv) {
  const urlElement = document.getElementById(URL_RABBITMQ_ID);
  urlElement.innerHTML = rabbitmqUrls[selectedEnv];
}


export function renderClientsConfigTable(clientsConfig) {
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

export function createVhostCardElement(vhost) {
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

export function updateEnvButtonStyles(selectedEnv) {
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

export function updateFiltersSelectOptions() {
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

export function getPerimeterFromVhost(vhost) {
  return vhost.split("_v")[0];
}