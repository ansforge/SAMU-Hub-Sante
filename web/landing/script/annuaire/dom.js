import {
  CLIENTS_CONFIG_TABLE_ID,
  perimeterInVhost,
  mddMap,
  RABBITMQ_URL,
  colors,
  perimeter,
} from "./constants.js";
import { FILTERS_CONFIG } from "./filters.js";
import { getDepartmentsInProd, getActorsFromDepartment } from "./data.js";

export function renderClientsConfigTable(clientsConfig) {
  const tableAnnuaireContent = document.getElementById(CLIENTS_CONFIG_TABLE_ID);
  tableAnnuaireContent.innerHTML = "";

  clientsConfig.forEach((item) => {
    const row = createClientConfigRow(item);
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
  document.getElementById("modal-clientID").innerHTML = item.client_id;
  document.getElementById("modal-env").innerHTML = "prod";
  const url_element = document.getElementById("modal-env-url");
  url_element.innerHTML = RABBITMQ_URL;
  url_element.href = RABBITMQ_URL;
  document.getElementById("modal-perimeter").innerHTML = perimeter;
  document.getElementById("modal-mdd").innerHTML = mdd;
  document.getElementById("modal-vhost").innerHTML = vhost;
}

export function onDepartmentSelected(dep) {
  unselectDepartment();
  document
    .querySelectorAll(`.department[data-num-dep='${dep.dataset.numDep}']`)
    .forEach((d) => d.classList.add("selected"));
  renderDepartmentInfo(dep);
}

export function unselectDepartment() {
  document
    .querySelectorAll(".department.selected")
    .forEach((d) => d.classList.remove("selected"));
}

export function hideInfoSelectedDepartment() {
  const divInfo = document.getElementById("department-infos");
  if (divInfo.classList.contains("d-block")) {
    divInfo.classList.replace("d-block", "d-none");
  }
}

function renderDepartmentInfo(dep) {
  const divInfo = document.getElementById("department-infos");
  divInfo.innerHTML = "";
  if (divInfo.classList.contains("d-none")) {
    divInfo.classList.replace("d-none", "d-block");
  }

  // Title
  divInfo.appendChild(createDepartmentTitle(dep));

  // Content
  if (!dep.classList.contains("prod")) {
    divInfo.style.backgroundColor = "var(--gray-500)";
    divInfo.appendChild(
      createDepartmentParagraph(
        "Aucune information disponible pour ce département en environnement de production.",
      ),
    );
  } else {
    const actors = getActorsFromDepartment(dep.dataset.numDep);
    divInfo.style.backgroundColor = "var(--primary)";
    const message =
      actors.length === 1
        ? "Un acteur a été trouvé :"
        : `${actors.length} acteurs ont été trouvés :`;
    divInfo.appendChild(createDepartmentParagraph(message));
    divInfo.appendChild(createActorsLinks(actors));
  }
}

function createDepartmentTitle(dep) {
  const title = document.createElement("h2");
  title.innerText = dep.querySelector("title").innerHTML;
  return title;
}

function createDepartmentParagraph(text) {
  const p = document.createElement("p");
  p.innerText = text;
  return p;
}

function createActorsLinks(actors) {
  const fragment = document.createDocumentFragment();
  actors.forEach((actor) => {
    const link = document.createElement("a");
    link.innerHTML = actor.label;
    link.style.color = "var(--white)";
    fragment.appendChild(link);
    fragment.appendChild(document.createElement("br"));
  });
  return fragment;
}

export function updateDepartmentInProdColor() {
  const departments = getDepartmentsInProd();
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
