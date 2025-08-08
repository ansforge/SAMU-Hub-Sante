import { fetchData, renameKeys, constituteVhostList, resetSelectedClientsConfig, create_data_test, getClientConfigByDepartment } from "./data.js";
import { renderClientsConfigTable, renderUrl, updateEnvButtonStyles, updateFiltersSelectOptions, openRecap, closeRecap, renderDepartmentInfo } from "./dom.js";
import { clientsConfigurations, Environment, apiUrls, RECAP_OPEN_BTN_ID, RECAP_CLOSE_BTN_ID } from "./constants.js";
import { getCurrentFilteredClientsConfig } from "./filters.js";
import { getSelectedEnv, setSelectedEnv } from "./env.js";

window.addEventListener("load", async () => {
  // for (const env of Object.values(Environment)) {
  //   const clientsConfig = await fetchData(apiUrls[env]);
  //   clientsConfigurations[env] = clientsConfig.map((item) => {
  //     const renamedData = renameKeys(item, keyMap);
  //     const vhostList = constituteVhostList(renamedData);
  //     return {
  //       ...renamedData,
  //       vhostList: vhostList,
  //       isSelected: false,
  //     };
  //   });
  // }
  setSelectedEnv(Environment.BAS);
  create_data_test();
  updateFiltersSelectOptions();
  const selectedEnv = getSelectedEnv();
  renderUrl(selectedEnv);
  renderClientsConfigTable(clientsConfigurations[selectedEnv]);
});

document.getElementById("env-buttons").addEventListener("click", (e) => {
  const btn = e.target.closest("button[data-env]");
  if (!btn) return;
  setSelectedEnv(btn.dataset.env);
  resetSelectedClientsConfig();
  const selectedEnv = getSelectedEnv();
  updateEnvButtonStyles(selectedEnv);
  updateFiltersSelectOptions(selectedEnv);
  renderUrl(selectedEnv);
  renderClientsConfigTable(clientsConfigurations[selectedEnv]);
});

document.querySelectorAll("#div-filtres select").forEach((select) => {
  select.addEventListener("change", () => {
    renderClientsConfigTable(getCurrentFilteredClientsConfig());
  });
});

document.getElementById(RECAP_OPEN_BTN_ID).addEventListener("click", () => {
  openRecap();
});

document.getElementById(RECAP_CLOSE_BTN_ID).addEventListener("click", () => {
  closeRecap();
});

document.querySelectorAll('.department').forEach(dep => {
  dep.addEventListener('click', () => {
    let selectedClientsConfig;
    if(dep.classList.contains('selected')) {
      dep.classList.remove('selected');
    } else {
      document.querySelectorAll('.department.selected').forEach(d => d.classList.remove('selected'));
      dep.classList.add('selected');
      selectedClientsConfig = getClientConfigByDepartment(dep.dataset.numDep);
    }    
    renderDepartmentInfo(selectedClientsConfig);
  });
});