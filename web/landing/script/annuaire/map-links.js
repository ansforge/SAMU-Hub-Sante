// Carte des acteurs : filtre par lien, pastilles et légende.
import { perimeterLabels } from "./constants.js";
import { getActorsFromDepartment, getDepartmentsInProd } from "./data.js";

// Liens proposés (dans l'ordre d'affichage) et couleur de leur pastille :
// charte ANS (blanc, couleur secondaire, couleurs « illustratives »).
const LINK_COLORS = {
  "15-15": "#FFFFFF",
  "15-nexsis": "#D20050",
  "15-smur": "#4EC79C",
  "15-cap": "#F29A2B",
  "15-cnr114": "#751F51",
  "15-portail": "#FAE832",
};
const LINKS = Object.keys(LINK_COLORS);

const SVG_NS = "http://www.w3.org/2000/svg";
const DOT_RADIUS = 4;
const DOT_SPACING = 9.5;
const DOT_MIN_AREA_SIZE = 9; // en dessous, le département est trop petit pour des pastilles

const LEGEND_ITEMS = [
  ["has-links", "Au moins un lien actif"],
  ["no-links", "Aucun lien actif"],
];

// Liens sélectionnés dans le filtre (vide = « Tous »).
const selectedLinks = new Set();
// Appelé à chaque clic sur un filtre (ex. : désélectionner le département en cours).
let onFilterChange = () => {};

export function initMapLinks(options = {}) {
  onFilterChange = options.onFilterChange ?? onFilterChange;
  renderPerimeterFilter();
  renderMapLegend();
}

/* ---------- Données ---------- */

function getActiveLinks(client) {
  return LINKS.filter((link) => client.perimeters?.[link]);
}

function getDepartmentLinks(numDep) {
  const links = new Set(getActorsFromDepartment(numDep).flatMap(getActiveLinks));
  return LINKS.filter((link) => links.has(link));
}

// Liens actifs dans au moins un département : les autres n'auraient aucun résultat.
function getAvailableLinks() {
  const links = new Set(getDepartmentsInProd().flatMap(getDepartmentLinks));
  return LINKS.filter((link) => links.has(link));
}

function hasAllSelectedLinks(numDep) {
  const links = getDepartmentLinks(numDep);
  return [...selectedLinks].every((link) => links.includes(link));
}

/* ---------- Filtre par lien ---------- */

function renderPerimeterFilter() {
  const container = document.getElementById("perimeter-filter");
  container.replaceChildren(
    createFilterButton(null, "Tous"),
    ...getAvailableLinks().map((link) => createFilterButton(link, perimeterLabels[link])),
  );
  updateMap();
}

function createFilterButton(link, label) {
  const button = createElement("button");
  button.type = "button";
  button.dataset.link = link ?? "";
  if (link) button.appendChild(createLinkDot(link));
  button.append(label);
  button.addEventListener("click", () => toggleLink(link));
  return button;
}

// « Tous » réinitialise ; un lien s'ajoute ou se retire de la sélection.
function toggleLink(link) {
  if (!link) selectedLinks.clear();
  else if (selectedLinks.has(link)) selectedLinks.delete(link);
  else selectedLinks.add(link);
  onFilterChange();
  updateMap();
}

function updateMap() {
  document.querySelectorAll("#perimeter-filter button").forEach((button) => {
    const link = button.dataset.link;
    const active = link ? selectedLinks.has(link) : selectedLinks.size === 0;
    button.className = `btn btn-sm btn--primary ${active ? "btn--plain" : "btn--ghost"}`;
    button.setAttribute("aria-pressed", String(active));
  });

  // Filtre combiné (ET) : seuls les départements ayant tous les liens sélectionnés restent en couleur.
  document.querySelectorAll("#div-map .department").forEach((dep) => {
    dep.classList.toggle("is-dimmed", selectedLinks.size > 0 && !hasAllSelectedLinks(dep.dataset.numDep));
  });
  renderLinkDots(document.querySelector("#div-map svg"));
}

/* ---------- Pastilles sur la carte ---------- */

// Une pastille par lien sélectionné dans chaque département qui les a tous.
function renderLinkDots(svg) {
  svg.querySelector("#link-dots")?.remove();
  if (selectedLinks.size === 0) return;

  const links = LINKS.filter((link) => selectedLinks.has(link));
  const layer = document.createElementNS(SVG_NS, "g");
  layer.id = "link-dots";
  layer.setAttribute("aria-hidden", "true");

  // Paris et petite couronne : pastilles uniquement dans l'encart zoomé.
  const zoomedDeps = new Set(
    [...svg.querySelectorAll('.department[id$="zoom"]')].map((dep) => dep.dataset.numDep),
  );

  svg.querySelectorAll(".department.prod:not(.is-dimmed)").forEach((dep) => {
    if (zoomedDeps.has(dep.dataset.numDep) && !dep.id.endsWith("zoom")) return;
    const anchor = getDepartmentAnchor(dep);
    if (!anchor) return;
    getDotPositions(links.length, anchor).forEach(({ x, y }, i) => {
      const dot = document.createElementNS(SVG_NS, "circle");
      dot.setAttribute("cx", x);
      dot.setAttribute("cy", y);
      dot.setAttribute("r", DOT_RADIUS);
      dot.setAttribute("fill", LINK_COLORS[links[i]]);
      layer.appendChild(dot);
    });
  });
  svg.appendChild(layer);
}

// Pastilles centrées sur 1 rangée (jusqu'à 3) ou 2 rangées.
function getDotPositions(count, center) {
  const rows = count <= 3 ? [count] : [Math.ceil(count / 2), Math.floor(count / 2)];
  return rows.flatMap((rowCount, row) =>
    Array.from({ length: rowCount }, (_, col) => ({
      x: center.x + (col - (rowCount - 1) / 2) * DOT_SPACING,
      y: center.y + (row - (rows.length - 1) / 2) * DOT_SPACING,
    })),
  );
}

// Point intérieur le plus éloigné des bords du département (mis en cache).
const anchorCache = new WeakMap();
function getDepartmentAnchor(dep) {
  if (!anchorCache.has(dep)) {
    const anchors = [...dep.querySelectorAll("path, polygon")].map(getPathAnchor).filter(Boolean);
    anchorCache.set(dep, anchors.sort((a, b) => b.distance - a.distance)[0] ?? null);
  }
  return anchorCache.get(dep);
}

function getPathAnchor(path) {
  const box = path.getBBox();
  if (Math.max(box.width, box.height) < DOT_MIN_AREA_SIZE) return null;

  const steps = 14;
  const length = path.getTotalLength();
  const border = Array.from({ length: 80 }, (_, i) => path.getPointAtLength((length * i) / 80));
  const point = path.ownerSVGElement.createSVGPoint();
  let best = null;
  for (let i = 1; i < steps; i++) {
    for (let j = 1; j < steps; j++) {
      point.x = box.x + (box.width * i) / steps;
      point.y = box.y + (box.height * j) / steps;
      if (!path.isPointInFill(point)) continue;
      const distance = Math.min(...border.map((b) => Math.hypot(b.x - point.x, b.y - point.y)));
      if (!best || distance > best.distance) best = { x: point.x, y: point.y, distance };
    }
  }
  return best;
}

/* ---------- Légende ---------- */

function renderMapLegend() {
  const container = document.getElementById("map-legend");
  const items = createElement("div", "map-legend__items");
  LEGEND_ITEMS.forEach(([modifier, label]) => {
    const item = createElement("span");
    item.append(createElement("span", `map-legend__swatch map-legend__swatch--${modifier}`), label);
    items.appendChild(item);
  });
  container.replaceChildren(items);
}

/* ---------- Encart du département ---------- */

// Liens actifs de chaque acteur du département, en badges.
export function createDepartmentLinks(numDep) {
  const fragment = document.createDocumentFragment();
  getActorsFromDepartment(numDep).forEach((actor) => {
    const links = getActiveLinks(actor);
    const list = createElement("ul", "list-unstyled link-badges");
    if (links.length === 0) list.append("Raccordé, aucun lien actif.");
    links.forEach((link) => {
      const item = createElement("li");
      item.appendChild(createLinkBadge(link));
      list.appendChild(item);
    });
    fragment.appendChild(list);
  });
  return fragment;
}

/* ---------- Éléments communs ---------- */

function createLinkBadge(link) {
  const badge = createElement("span", "link-badge");
  badge.append(createLinkDot(link), perimeterLabels[link]);
  return badge;
}

function createLinkDot(link) {
  const dot = createElement("span", "link-dot");
  dot.style.backgroundColor = LINK_COLORS[link];
  return dot;
}

function createElement(tag, className, text) {
  const element = document.createElement(tag);
  if (className) element.className = className;
  if (text !== undefined) element.textContent = text;
  return element;
}
