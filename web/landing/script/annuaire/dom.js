import {
  CLIENTS_CONFIG_TABLE_ID,
  perimeterInVhost,
  mddMap,
  rabbitmqUrls,
  colors,
  perimeter,
} from "./constants.js";
import { FILTERS_CONFIG } from "./filters.js";
import { getDepartmentInProd, getClientConfigByDepartment } from "./data.js";
import { getSelectedEnv } from "./env.js";

export function renderClientsConfigTable(clientsConfig) {
  const tableAnnuaireContent = document.getElementById(CLIENTS_CONFIG_TABLE_ID);
  tableAnnuaireContent.innerHTML = "";

  clientsConfig.forEach((item, index) => {
    const row = createClientConfigRow(item, index);
    tableAnnuaireContent.appendChild(row);
  });
}

function createClientConfigRow(item) {
  const row = document.createElement("tr");
  row.appendChild(createTextCell(item.client_id));
  row.appendChild(createTextCell(item.editor));
  row.appendChild(createAuthorizedPerimetersCell(item));
  return row;
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
      if (version) {
        const vhost = `${perimeterInVhost[perimeter]}_v${version}`;
        const mdd = mddMap[vhost];
        const perimeterElement = createAuthorizedPerimetersElement(
          perimeter,
          mdd,
          item,
          vhost,
        );
        td.appendChild(perimeterElement);
      }
    });
  });
  return td;
}

function createAuthorizedPerimetersElement(perimeter, mdd, item, vhost) {
  const element = document.createElement("a");
  element.classList.add("btn", "btn--ghost", "btn--default", "btn-sm");
  element.style.borderColor = colors[perimeter];
  element.dataset.toggle = "modal";
  element.dataset.target = "#modal1";
  element.textContent = `${perimeter} (${mdd})`;
  element.addEventListener("click", (e) => {
    e.preventDefault();
    fillModaleInfo(vhost, perimeter, mdd, item);
  });
  return element;
}

function fillModaleInfo(vhost, perimeter, mdd, item) {
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

export function onDepartmentSelected(dep) {
  document
    .querySelectorAll(".department.selected")
    .forEach((d) => d.classList.remove("selected"));
  document
    .querySelectorAll(`.department[data-num-dep='${dep.dataset.numDep}']`)
    .forEach((d) => d.classList.add("selected"));
  renderDepartmentInfo(dep);
}

function renderDepartmentInfo(dep) {
  const divInfo = document.getElementById("department-infos");
  divInfo.innerHTML = "";
  if (divInfo.classList.contains("d-none")) {
    divInfo.classList.replace("d-none", "d-block");
  }
  const departmentLabel = dep.querySelector("title").innerHTML;
  const title = document.createElement("h2");
  title.innerText = departmentLabel;
  divInfo.appendChild(title);
  if (!dep.classList.contains("prod")) {
    divInfo.style.backgroundColor = "var(--gray-500)";
    const p = document.createElement("p");
    p.innerText =
      "Aucune information disponible pour ce département en environnement de production.";
    divInfo.appendChild(p);
  } else {
    divInfo.style.backgroundColor = "var(--primary)";
    const test = ["SAMU", "SNP"];
    const p = document.createElement("p");
    p.innerText = `${test.length} acteurs ont été trouvés :`;
    divInfo.appendChild(p);
    test.forEach((item) => {
      const link = document.createElement("a");
      link.innerHTML = item;
      link.href = "#";
      link.style.color = "var(--white)";
      divInfo.appendChild(link);
      divInfo.appendChild(document.createElement("br"));
    });
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
