import { fetchData, renameKeys, constituteVhostList, create_data_test } from "./data.js";
import { renderClientsConfigTable, renderUrl, updateEnvButtonStyles, updateFiltersSelectOptions } from "./dom.js";
import { clientsConfigurations, Environment, apiUrls } from "./constants.js";
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

