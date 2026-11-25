import {
  CLIENTS_CONFIG_TABLE_ID,
  perimeterInVhost,
  perimeterLabels,
  perimeterPdfLinks,
  perimeter,
} from "./constants.js";
import { FILTERS_CONFIG } from "./filters.js";
import {
  getDepartmentsInProd,
  getActorsFromDepartment,
  sortClientConfig,
  getActorsInfo,
  getDepartmentLinks,
} from "./data.js";

export function renderClientsConfigTable(clientsConfig) {
  const sortedClientConfig = sortClientConfig(clientsConfig);
  const tableAnnuaireContent = document.getElementById(CLIENTS_CONFIG_TABLE_ID);
  tableAnnuaireContent.innerHTML = "";

  sortedClientConfig.forEach((item) => {
    const row = createClientConfigRow(item);
    tableAnnuaireContent.appendChild(row);
  });
}

function createClientConfigRow(item) {
  const row = document.createElement("tr");
  row.appendChild(createTextCell(item.label));
  row.appendChild(createTextCell(item.client_id));
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
  perimeter.forEach((p) => {
    if (item.perimeters?.[p]) {
      const vhost = perimeterInVhost[p];
      const perimeterElement = createAuthorizedPerimetersElement(p, item, vhost);
      td.appendChild(perimeterElement);
    }
  });
  return td;
}

function createAuthorizedPerimetersElement(perimeter, item, vhost) {
  const element = document.createElement("a");
  element.classList.add("btn", "btn--ghost", "btn--default", "btn-sm");
  element.textContent = perimeterLabels[perimeter];
  const pdfLink = perimeterPdfLinks[perimeter];
  if (pdfLink) {
    element.href = pdfLink;
    element.target = "_blank";
  } else {
    element.style.cursor = "default";
    element.style.opacity = "0.6";
  }
  return element;
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
  divInfo.appendChild(createTitleH2(dep));

  // Content
  if (!dep.classList.contains("prod")) {
    divInfo.style.backgroundColor = "var(--gray-500)";
    divInfo.appendChild(
      createParagraph(
        "Aucune information disponible pour ce département en environnement de production.",
      ),
    );
  } else {
    const actors_info = getActorsFromDepartment(dep.dataset.numDep).map((c) =>
      getActorsInfo(c),
    );
    divInfo.style.backgroundColor = "var(--primary)";
    divInfo.appendChild(createUnorderedList(actors_info));
  }
}

function createTitleH2(dep) {
  const title = document.createElement("h2");
  title.innerText = dep.querySelector("title").innerHTML;
  return title;
}

function createParagraph(text) {
  const p = document.createElement("p");
  p.innerText = text;
  return p;
}

function createUnorderedList(list) {
  const ul = document.createElement("ul");
  list.forEach((item) => {
    const li = document.createElement("li");
    li.textContent = item;
    ul.appendChild(li);
  });
  return ul;
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

// Filtre par lien sur la carte
const SVG_NS = "http://www.w3.org/2000/svg";
const PETITE_COURONNE = ["75", "92", "93", "94"];
const DOT_RADIUS = 4;
const DOT_STEP = 9.5;
// En dessous, le département est trop petit pour des pastilles
const MIN_DEPARTMENT_SIZE = 9;

const mapFilter = { selectedLinks: [] };

export function renderLinkFilters() {
  const container = document.getElementById("link-filters");
  container.innerHTML = "";
  const departments = getDepartmentsInProd();
  const availableLinks = perimeter.filter((p) =>
    departments.some((dep) => getDepartmentLinks(dep).includes(p)),
  );
  container.appendChild(createLinkFilterButton("Tous", ""));
  availableLinks.forEach((p) =>
    container.appendChild(createLinkFilterButton(perimeterLabels[p], p)),
  );
  updateLinkFilterButtons();
}

function createLinkFilterButton(label, link) {
  const button = document.createElement("button");
  button.type = "button";
  button.classList.add("btn", "btn-sm", "btn--primary", "link-filter");
  button.dataset.link = link;
  if (link) {
    button.appendChild(createLinkDot(link));
  }
  button.appendChild(document.createTextNode(label));
  return button;
}

function createLinkDot(link) {
  const dot = document.createElement("span");
  dot.classList.add("link-dot");
  dot.classList.add(`perimeter-${link}`);
  return dot;
}

function updateLinkFilterButtons() {
  const selected = mapFilter.selectedLinks;
  document.querySelectorAll("#link-filters .link-filter").forEach((button) => {
    const link = button.dataset.link;
    const active = link ? selected.includes(link) : selected.length === 0;
    button.setAttribute("aria-pressed", active);
    button.classList.toggle("btn--plain", active);
    button.classList.toggle("btn--ghost", !active);
  });
}

export function onLinkFilterClicked(link) {
  const selected = mapFilter.selectedLinks;
  if (!link) {
    mapFilter.selectedLinks = [];
  } else if (selected.includes(link)) {
    mapFilter.selectedLinks = selected.filter((l) => l !== link);
  } else {
    mapFilter.selectedLinks = perimeter.filter(
      (p) => p === link || selected.includes(p),
    );
  }
  resetDepartmentSelection();
  updateLinkFilterButtons();
  applyLinkFilter();
}

export function resetDepartmentSelection() {
  unselectDepartment();
  hideInfoSelectedDepartment();
  document.getElementById("departements-list").value = "";
}

function applyLinkFilter() {
  const selected = mapFilter.selectedLinks;
  document.querySelectorAll("#div-map .link-dots").forEach((g) => g.remove());
  document.querySelectorAll("#div-map .department.prod").forEach((dep) => {
    const links = getDepartmentLinks(dep.dataset.numDep);
    const keep = selected.every((l) => links.includes(l));
    dep.classList.toggle("filtered-out", !keep);
    const isMainPetiteCouronne =
      PETITE_COURONNE.includes(dep.dataset.numDep) &&
      dep.closest("#complete_map");
    if (keep && selected.length > 0 && !isMainPetiteCouronne) {
      renderDots(dep, selected);
    }
  });
}

function renderDots(dep, links) {
  const anchor = getDepartmentAnchor(dep);
  if (!anchor) return;
  const half = Math.ceil(links.length / 2);
  const rows =
    links.length > 3 ? [links.slice(0, half), links.slice(half)] : [links];

  const group = document.createElementNS(SVG_NS, "g");
  group.classList.add("link-dots");
  rows.forEach((row, i) => {
    const y = anchor.y + (i - (rows.length - 1) / 2) * DOT_STEP;
    row.forEach((link, j) => {
      const circle = document.createElementNS(SVG_NS, "circle");
      const x = anchor.x + (j - (row.length - 1) / 2) * DOT_STEP;
      circle.setAttribute("cx", x);
      circle.setAttribute("cy", y);
      circle.setAttribute("r", DOT_RADIUS);
      circle.classList.add(`perimeter-${link}`);
      group.appendChild(circle);
    });
  });
  // En fin de SVG : les pastilles passent au-dessus de tous les départements
  dep.ownerSVGElement.appendChild(group);
}

// Point intérieur le plus éloigné des bords du département (mis en cache).
const anchorCache = new WeakMap();
function getDepartmentAnchor(dep) {
  if (!anchorCache.has(dep)) {
    const anchors = [...dep.querySelectorAll("path, polygon")]
      .map(getPathAnchor)
      .filter(Boolean);
    anchorCache.set(
      dep,
      anchors.sort((a, b) => b.distance - a.distance)[0] ?? null,
    );
  }
  return anchorCache.get(dep);
}

function getPathAnchor(path) {
  const box = path.getBBox();
  if (Math.max(box.width, box.height) < MIN_DEPARTMENT_SIZE) return null;

  const steps = 14;
  const length = path.getTotalLength();
  const border = Array.from({ length: 80 }, (_, i) =>
    path.getPointAtLength((length * i) / 80),
  );
  const point = path.ownerSVGElement.createSVGPoint();
  let best = null;
  for (let i = 1; i < steps; i++) {
    for (let j = 1; j < steps; j++) {
      point.x = box.x + (box.width * i) / steps;
      point.y = box.y + (box.height * j) / steps;
      if (!path.isPointInFill(point)) continue;
      const distance = Math.min(
        ...border.map((b) => Math.hypot(b.x - point.x, b.y - point.y)),
      );
      if (!best || distance > best.distance) {
        best = { x: point.x, y: point.y, distance };
      }
    }
  }
  return best;
}
