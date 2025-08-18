import {
  fetchData,
  renameKeys,
  constituteVhostList,
  resetSelectedClientsConfig,
  create_data_test,
} from "./data.js";
import {
  renderClientsConfigTable,
  updateEnvButtonStyles,
  updateFiltersSelectOptions,
  openRecap,
  closeRecap,
  renderDepartmentInfo,
  updateDepartmentInProdColor,
  handleClickOnDepartment,
} from "./dom.js";
import {
  clientsConfigurations,
  Environment,
  apiUrls,
  RECAP_OPEN_BTN_ID,
  RECAP_CLOSE_BTN_ID,
  DIV_MAP_ID,
  TOOLTIP_INFO_VHOST_ID,
  TOOLTIP_IMAGE_VHOST_ID,
} from "./constants.js";
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
  setSelectedEnv(Environment.PROD);
  create_data_test();
  updateDepartmentInProdColor();
  updateFiltersSelectOptions();
  const selectedEnv = getSelectedEnv();
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

document.getElementById(DIV_MAP_ID).addEventListener("click", (e) => {
  const dep = e.target.closest(".department");
  if (!dep) return;
  handleClickOnDepartment(dep);
});

const tooltipInfoVhost = document.getElementById(TOOLTIP_INFO_VHOST_ID);
const tooltipImgVhost = document.getElementById(TOOLTIP_IMAGE_VHOST_ID);
tooltipInfoVhost.addEventListener("click", (e) => {
  tooltipImgVhost.classList.toggle("hidden");
});
document.addEventListener("click", (e) => {
  if (
    !tooltipInfoVhost.contains(e.target) &&
    !tooltipImgVhost.contains(e.target)
  ) {
    tooltipImgVhost.classList.add("hidden");
  }
});
