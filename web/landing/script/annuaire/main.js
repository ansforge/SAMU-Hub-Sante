import { fetchData, renameKeys, constituteVhostList } from "./data.js";
import {
  renderClientsConfigTable,
  updateEnvButtonStyles,
  updateFiltersSelectOptions,
  updateDepartmentInProdColor,
  onDepartmentSelected,
  hideInfoSelectedDepartment,
  unselectDepartment,
} from "./dom.js";
import {
  keyMap,
  clientsConfigurations,
  Environment,
  apiUrls,
  DIV_MAP_ID,
} from "./constants.js";
import { getCurrentFilteredClientsConfig } from "./filters.js";
import { getSelectedEnv, setSelectedEnv } from "./env.js";

window.addEventListener("load", async () => {
  for (const env of Object.values(Environment)) {
    const clientsConfig = await fetchData(apiUrls[env]);
    clientsConfigurations[env] = clientsConfig.map((item) => {
      const renamedData = renameKeys(item, keyMap);
      const vhostList = constituteVhostList(renamedData);
      return {
        ...renamedData,
        vhostList: vhostList,
      };
    });
  }
  setSelectedEnv(Environment.PROD);
  updateDepartmentInProdColor();
  updateFiltersSelectOptions();
  const selectedEnv = getSelectedEnv();
  renderClientsConfigTable(clientsConfigurations[selectedEnv]);
});

document.getElementById("env-buttons").addEventListener("click", (e) => {
  const btn = e.target.closest("button[data-env]");
  if (!btn) return;
  setSelectedEnv(btn.dataset.env);
  const selectedEnv = getSelectedEnv();
  updateEnvButtonStyles(selectedEnv);
  updateFiltersSelectOptions(selectedEnv);
  renderClientsConfigTable(clientsConfigurations[selectedEnv]);
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
