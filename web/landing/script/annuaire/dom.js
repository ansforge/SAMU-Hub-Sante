import {
  CLIENTS_CONFIG_TABLE_ID,
  perimeterInVhost,
  mddMap,
  rabbitmqUrls,
  colors,
  DIV_INFO_DEPARTMENT_ID,
  perimeter,
} from "./constants.js";
import { FILTERS_CONFIG, getCurrentFilteredClientsConfig } from "./filters.js";
import {
  getDepartmentInProd,
  getSelectedClientsConfig,
  getClientConfigByDepartment,
} from "./data.js";
import { getSelectedEnv } from "./env.js";

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
  row.appendChild(createAuthorizedPerimetersCell(item));
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

function createTextCell(text) {
  const td = document.createElement("td");
  td.textContent = text;
  return td;
}

function createAuthorizedPerimetersCell(item) {
  const td = document.createElement("td");
  td.style.display = "flex";
  td.style.flexWrap = "wrap";
  td.style.gap = "5px";
  perimeter.forEach((perimeter) => {
    item[perimeter].split(",").forEach((version) => {
      if(version){
        const vhost = `${perimeterInVhost[perimeter]}_v${version}`;
        const mdd = mddMap[vhost];
        const perimeterElement = createAuthorizedPerimetersElement(perimeter, mdd, item, vhost);
        td.appendChild(perimeterElement);
      }
    })
  });
  return td;
}

function createAuthorizedPerimetersElement(perimeter, mdd, item, vhost) {
  const element = document.createElement("a");
  element.classList.add("btn", "btn--ghost", "btn--default", "btn-sm");
  element.dataset.toggle="modal";
  element.dataset.target="#modal1";
  element.style.borderColor = colors[perimeter];
  element.textContent = `${perimeter} (${mdd})`;
  element.addEventListener("click", (e) => {
      e.preventDefault();
      fillModaleInfo(vhost, perimeter, mdd, item);
    });
  return element;
}

function fillModaleInfo(vhost,perimeter, mdd, item){
  const env = getSelectedEnv();
  const rabbitmqUrl = rabbitmqUrls[env];
  document.getElementById("modal-clientID").innerHTML = item.client_id;
  document.getElementById("modal-env").innerHTML = env;
  const url_element = document.getElementById("modal-env-url");
  url_element.innerHTML = rabbitmqUrl;
  url_element.href = rabbitmqUrl;
  document.getElementById("modal-perimeter").innerHTML = perimeter;
  document.getElementById("modal-mdd").innerHTML = mdd;
  document.getElementById("modal-vhost").innerHTML = vhost;
}

export function handleClickOnDepartment(dep) {
  if (!dep.classList.contains("prod")) return;
  let selectedClientsConfig;
  if (dep.classList.contains("selected")) {
    dep.classList.remove("selected");
  } else {
    document
      .querySelectorAll(".department.selected")
      .forEach((d) => d.classList.remove("selected"));
    dep.classList.add("selected");
    selectedClientsConfig = getClientConfigByDepartment(dep.dataset.numDep);
  }
  renderDepartmentInfo(selectedClientsConfig);
}

export function renderDepartmentInfo(clientConfig) {
  const infoSelectedDepartment = document.getElementById(
    DIV_INFO_DEPARTMENT_ID,
  );
  infoSelectedDepartment.innerHTML = "";
  if (clientConfig) {
    const clientConfigCard = createClientConfigCard(clientConfig);
    infoSelectedDepartment.appendChild(clientConfigCard);
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

export function updateDepartmentInProdColor() {
  const departments = getDepartmentInProd();
  departments.forEach((dep) => {
    const elements = document.querySelectorAll(`[data-num-dep="${dep}"]`);
    elements.forEach((elem) => elem.classList.add("prod"));
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
