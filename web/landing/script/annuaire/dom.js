import { URL_RABBITMQ_ID, CLIENTS_CONFIG_TABLE_ID, RECAP_CONTENT_ID, mddMap, rabbitmqUrls, colors, RECAP_CONTAINER_ID, RECAP_OPEN_BTN_ID } from "./constants.js";
import { FILTERS_CONFIG, getCurrentFilteredClientsConfig } from "./filters.js";
import { getSelectedClientsConfig } from "./data.js";
import { getSelectedEnv } from "./env.js";

export function renderUrl(selectedEnv) {
  const urlElement = document.getElementById(URL_RABBITMQ_ID);
  urlElement.innerHTML = `URL: ${rabbitmqUrls[selectedEnv]}`;
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
				updateRecapButtonState();
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
  vhostDiv.classList.add("vhost-card");
  vhostDiv.style.backgroundColor = colors[getPerimeterFromVhost(vhost)];

  const strong = document.createElement("strong");
  strong.textContent = vhost;
  vhostDiv.appendChild(strong);
  vhostDiv.appendChild(document.createElement("br"));
  const mdd = document.createElement("div");
  mdd.textContent = mddMap[vhost];
  vhostDiv.appendChild(mdd);

  return vhostDiv;
}

export function openRecap() {
  const selectedClientsConfig = getSelectedClientsConfig();
	renderRecap(selectedClientsConfig);
}

function renderRecap(selectedClientsConfig) {
	const recapContainer = document.getElementById(RECAP_CONTAINER_ID);
	const recapContent = document.getElementById(RECAP_CONTENT_ID);
	recapContent.innerHTML = "";

  const url = document.createElement("p");
  url.classList.add("recap-url");
  url.textContent = `URL : ${rabbitmqUrls[getSelectedEnv()]}`;
  recapContent.appendChild(url);

	selectedClientsConfig.forEach(clientConfig => {
    const clientConfigCard = createClientConfigCard(clientConfig);
    recapContent.appendChild(clientConfigCard);
  });
  recapContainer.style.display = "flex";
}

function createClientConfigCard(clientConfig){
  const clientConfigCard = document.createElement("div");
  clientConfigCard.classList.add("client-config-card");

  const clientID = document.createElement("h3");
  clientID.textContent = `Identifiant client : ${clientConfig.client_id}`;
  clientConfigCard.appendChild(clientID);

  const editor = document.createElement("p");
  editor.textContent = "Editeur : ";
  const strong = document.createElement("strong");
  strong.textContent = clientConfig.editor;
  editor.appendChild(strong);
  clientConfigCard.appendChild(editor);

  const vhostDiv = document.createElement("div");
  vhostDiv.classList.add("recap-div-vhost");
  clientConfig.vhostList.forEach((vhost) => {
    const vhostCard = createVhostCardElement(vhost);
    vhostDiv.appendChild(vhostCard);
  });
  clientConfigCard.appendChild(vhostDiv);

  return clientConfigCard;
}

export function closeRecap() {
	const recapContainer = document.getElementById(RECAP_CONTAINER_ID);
	recapContainer.style.display = "none";
}

function updateRecapButtonState() {
	const recapButton = document.getElementById(RECAP_OPEN_BTN_ID);
	if(getSelectedClientsConfig().length == 2) {
		recapButton.disabled = false;
	} else {
		recapButton.disabled = true;
	}
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