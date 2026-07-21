import { fetchData, constituteLabel, state } from "./data.js";
import {
  renderClientsConfigTable,
  updateFiltersSelectOptions,
  updateDepartmentInProdColor,
  onDepartmentSelected,
  hideInfoSelectedDepartment,
  unselectDepartment,
} from "./dom.js";
import { API_URL } from "./constants.js";
import { getCurrentFilteredClientsConfig } from "./filters.js";

window.addEventListener("load", async () => {
  const svgResponse = await fetch("/img/carte-france.svg");
  const svgContent = await svgResponse.text();
  document.getElementById("div-map").innerHTML = svgContent;
  const clientsConfig = await fetchData(API_URL);
state.clientsConfigurations = clientsConfig.map((item) => ({
    ...item,
    label: constituteLabel(item),
  }));
  updateDepartmentInProdColor();
  updateFiltersSelectOptions();
  renderClientsConfigTable(state.clientsConfigurations);
});

document.querySelectorAll("#div-filtres select").forEach((select) => {
  select.addEventListener("change", () => {
    renderClientsConfigTable(getCurrentFilteredClientsConfig());
  });
});

document.getElementById("div-map").addEventListener("click", (e) => {
  const dep = e.target.closest(".department");
  if (!dep) return;
  document.getElementById("departements-list").value = dep.dataset.numDep;
  onDepartmentSelected(dep);
});

document.getElementById("dep-sel-btn").addEventListener("click", () => {
  const selectedDepValue = document.getElementById("departements-list").value;
  if (selectedDepValue === "") {
    unselectDepartment();
    hideInfoSelectedDepartment();
  } else {
    const dep = document.querySelector(`[data-num-dep="${selectedDepValue}"]`);
    onDepartmentSelected(dep);
  }
});
