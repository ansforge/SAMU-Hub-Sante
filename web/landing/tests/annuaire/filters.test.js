import {
  getActors,
  getPerimeter,
  getCurrentFilteredClientsConfig,
} from "../../script/annuaire/filters.js";
import { state } from "../../script/annuaire/data.js";
import { FILTER_IDS } from "../../script/annuaire/constants.js";

function perimeters(active) {
  return {
    "15-15": false,
    "15-nexsis": false,
    "15-smur": false,
    "15-gps": false,
    "15-cap": false,
    "15-cnr114": false,
    "15-portail": false,
    ...Object.fromEntries(active.map((p) => [p, true])),
  };
}

describe("Filters utils", () => {
  beforeEach(() => {
    state.clientsConfigurations = [
      {
        client_id: "fr.health.lrm",
        label: "",
        perimeters: perimeters(["15-15", "15-nexsis"]),
      },
      {
        client_id: "fr.health.snp410",
        label: "SNP 41",
        perimeters: perimeters(["15-15"]),
      },
      {
        client_id: "fr.health.samu410",
        label: "SAMU 41",
        perimeters: perimeters(["15-15", "15-smur"]),
      },
      {
        client_id: "fr.health.fire",
        label: "",
        perimeters: perimeters(["15-nexsis"]),
      },
    ];
  });

  test("getActors should return unique non-empty labels", () => {
    expect(getActors()).toEqual(["SNP 41", "SAMU 41"]);
  });

  test("getPerimeter should only return perimeters active for at least one client", () => {
    expect(getPerimeter()).toEqual(["15-15", "15-nexsis", "15-smur"]);
  });

  test("getPerimeter should return nothing when no client has an active perimeter", () => {
    state.clientsConfigurations = [
      { client_id: "fr.health.lrm", label: "", perimeters: perimeters([]) },
      { client_id: "fr.health.other", label: "" },
    ];
    expect(getPerimeter()).toEqual([]);
  });

  describe("getCurrentFilteredClientsConfig", () => {
    beforeEach(() => {
      // Mock expected HTML inputs
      document.body.innerHTML = `
        <select id="${FILTER_IDS.actor}">
          <option value=""></option>
          <option value="SNP 41">SNP 41</option>
          <option value="SAMU 41">SAMU 41</option>
        </select>
        <select id="${FILTER_IDS.perimeter}">
          <option value=""></option>
          <option value="15-15">15-15</option>
          <option value="15-nexsis">15-nexsis</option>
          <option value="15-smur">15-smur</option>
        </select>
      `;
    });

    test("should return all configs when no filter is selected", () => {
      expect(getCurrentFilteredClientsConfig()).toEqual(
        state.clientsConfigurations,
      );
    });

    test("should filter by actor", () => {
      document.getElementById(FILTER_IDS.actor).value = "SNP 41";
      expect(
        getCurrentFilteredClientsConfig().map((c) => c.client_id),
      ).toEqual(["fr.health.snp410"]);
    });

    test("should filter by perimeter", () => {
      document.getElementById(FILTER_IDS.perimeter).value = "15-nexsis";
      expect(
        getCurrentFilteredClientsConfig().map((c) => c.client_id),
      ).toEqual(["fr.health.lrm", "fr.health.fire"]);
    });

    test("should combine actor and perimeter filters", () => {
      document.getElementById(FILTER_IDS.actor).value = "SAMU 41";
      document.getElementById(FILTER_IDS.perimeter).value = "15-15";
      expect(
        getCurrentFilteredClientsConfig().map((c) => c.client_id),
      ).toEqual(["fr.health.samu410"]);
    });

    test("should return nothing when filters do not match together", () => {
      document.getElementById(FILTER_IDS.actor).value = "SNP 41";
      document.getElementById(FILTER_IDS.perimeter).value = "15-smur";
      expect(getCurrentFilteredClientsConfig()).toEqual([]);
    });
  });
});
