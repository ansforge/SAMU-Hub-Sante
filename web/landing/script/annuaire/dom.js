import { URL_RABBITMQ_ID, CLIENTS_CONFIG_TABLE_ID, mddMap, rabbitmqUrls, colors } from "./constants.js";
import { FILTERS_CONFIG, getCurrentFilteredClientsConfig } from "./filters.js";
import { getSelectedClientsConfig } from "./data.js";

export function renderUrl(selectedEnv) {
  const urlElement = document.getElementById(URL_RABBITMQ_ID);
  urlElement.innerHTML = rabbitmqUrls[selectedEnv];
}


export function renderClientsConfigTable(clientsConfig) {
  const tableAnnuaireContent = document.getElementById(CLIENTS_CONFIG_TABLE_ID);
  tableAnnuaireContent.innerHTML = "";

  clientsConfig.forEach((item, index) => {
    const row = createClientConfigRow(item, index);
    tableAnnuaireContent.appendChild(row);
  });
}

function createClientConfigRow(item, index) {
    const row = document.createElement("tr");
    row.appendChild(createCheckboxCell(index, item.isSelected));
    row.appendChild(createTextCell(item.client_id));
    row.appendChild(createTextCell(item.editor));
    row.appendChild(createVhostCell(item.vhostList));
    return row;
}

function createCheckboxCell(index, isSelected) {
    const td = document.createElement("td");
    td.style.textAlign = "center";
    const checkbox = document.createElement("input");
    checkbox.type = "checkbox";
    checkbox.dataset.index = index;
    checkbox.checked = !!isSelected;
    checkbox.addEventListener("change", function () {
        getCurrentFilteredClientsConfig()[index].isSelected = this.checked;
    });
    td.appendChild(checkbox);
    return td;
}

function createTextCell(text){
    const td = document.createElement("td");
    td.textContent = text;
    return td;
}

function createVhostCell(vhostList) {
    const td = document.createElement("td");
    td.style.display = "flex";
    td.style.flexWrap = "wrap";
    td.style.gap = "5px";
    vhostList.forEach((vhost) => {
      const vhostCard = createVhostCardElement(vhost);
      td.appendChild(vhostCard);
    });
    return td;
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